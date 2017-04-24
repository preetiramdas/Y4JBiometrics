package com.y4j.services;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.nitgen.SDK.BSP.NBioBSPJNI;
import com.y4j.pojo.FingerprintData;
import com.y4j.pojo.Users;

@Path("/y4j")
public class Main {

	// BioEnableWrapper wrapper = null;

	NBioBSPJNI nbsp = null;
	// MongoClient mongoCl = null;
	NBioBSPJNI.IndexSearch IndexSearchEngine;
	NBioBSPJNI.INPUT_FIR inputFIR;

	public Main() {

	}

	public static void main(String[] args) {
		Main m = new Main();

	}

	public boolean checkDeviceConnected() {
		nbsp = new NBioBSPJNI();
		NBioBSPJNI.DEVICE_ENUM_INFO device = nbsp.new DEVICE_ENUM_INFO();
		nbsp.EnumerateDevice(device);
		nbsp.OpenDevice();
		int iDevice = nbsp.GetOpenedDeviceID();
		if (iDevice == 0) {
			return false;
		}
		return true;

	}

	/**
	 * Enrolls the student and creates a record
	 * 
	 * @param userId
	 * @param userName
	 * @param fingerIdOpt
	 * @return
	 */
	@POST
	@Path("/takeBiometrics")
	@Produces(MediaType.TEXT_HTML)
	public Response takeBiometrics(@FormParam("userId") String userId, @FormParam("userName") String userName,
			@FormParam("fingerIdOpt") String fingerIdOpt) {
		// Getting device
		System.out.println("user::" + userId + " " + userName + " " + fingerIdOpt);
		nbsp = new NBioBSPJNI();
		NBioBSPJNI.DEVICE_ENUM_INFO device = nbsp.new DEVICE_ENUM_INFO();
		nbsp.EnumerateDevice(device);

		// Open Device
		nbsp.OpenDevice(device.DeviceInfo[0].NameID, device.DeviceInfo[0].Instance);

		// Enrollment and capture
		NBioBSPJNI.FIR_HANDLE hSavedFIR = nbsp.new FIR_HANDLE();
		nbsp.Enroll(hSavedFIR, null);

		if (nbsp.IsErrorOccured() == false) {
			NBioBSPJNI.FIR binaryFIR = nbsp.new FIR();
			nbsp.GetFIRFromHandle(hSavedFIR, binaryFIR);
			byte[] b = binaryFIR.Data;
			System.out.println("b=" + b);
		}

		NBioBSPJNI.FIR_TEXTENCODE textEncode = null;
		if (nbsp.IsErrorOccured() == false) {
			textEncode = nbsp.new FIR_TEXTENCODE();
			int i = nbsp.GetTextFIRFromHandle(hSavedFIR, textEncode);
			System.out.println("text fir=" + textEncode.TextFIR);
		}
		inputFIR = nbsp.new INPUT_FIR();

		inputFIR.SetTextFIR(textEncode);
		inputFIR.SetFIRHandle(hSavedFIR);
		System.out.println("input FIR=" + inputFIR.toString());

		MongoClient mongoCl = new MongoClient("localhost", 27017);
		writeFingerprint(mongoCl, userId, textEncode.TextFIR, Integer.parseInt(fingerIdOpt), userName);

		// Closing device
		nbsp.CloseDevice(device.DeviceInfo[0].NameID, device.DeviceInfo[0].Instance);

		nbsp.dispose();
		nbsp = null;
		// String response = "<span>Successfully enrolled trainee: " + userId +
		// "</span><br/><a href='index.jsp'>Home</a>";
		String response = "<script>alert('Successfully enrolled trainee');window.location.href='http://localhost:8080/Y4J/enroll.jsp';window.location.refresh();</script>";
		return Response.status(200).entity(response).build();

	}

	/**
	 * Writes the enrollment data to FIR collection in DB
	 * 
	 * @param mongo
	 * @param userId
	 *            :aadhar no.
	 * @param fingerPrintData
	 *            : byte data of fingerprint
	 */
	public void writeFingerprint(MongoClient mongo, String userId, String fingerPrintData, int fingerId, String name) {

		DB db = mongo.getDB("youthforjobs");
		DBCollection fingerprint = db.getCollection("FIR");
		BasicDBObjectBuilder docBuilder = BasicDBObjectBuilder.start();

		docBuilder.append("userId", userId);
		docBuilder.append("userName", name);
		docBuilder.append("fingerId", fingerId);
		docBuilder.append("fingerprintbytes", fingerPrintData);
		DBObject doc = docBuilder.get();
		WriteResult result = fingerprint.insert(doc);
		System.out.println("Record inserted in DB " + result.getUpsertedId());
	}

	/**
	 * Records attendance of the student
	 * 
	 * @return String
	 */
	@Path("/recordAttendance")
	@POST
	@Produces(MediaType.TEXT_HTML)
	public String takeAttendance(@FormParam("batchCodeAt") String name) {
		System.out.println("name::" + name);
		nbsp = new NBioBSPJNI();
		String status = "";
		int nMaxSearchTime = 0;

		// MongoClient mongoCl = MongoConnect.connectToDB();
		/*
		 * ServerAddress sr = new ServerAddress("localhost", 27017);
		 * MongoCredential mCred = MongoCredential.createCredential("admin",
		 * "youthforjobs", "admin".toCharArray()); List auth = new ArrayList();
		 * auth.add(mCred); MongoClient mongoCl = new MongoClient(sr, auth);
		 */
		MongoClient mongoCl = new MongoClient("localhost", 27017);

		// get the aadhar number of the trainee
		List<String> users = getTraineeDetails(mongoCl, name);
		System.out.println("list size: " + users.size());
		for (String entry : users) {

			List<FingerprintData> fingerprintData = readFingerPrintData(mongoCl, entry.trim());

			System.out.println("data from FIR =" + fingerprintData);
			if (!fingerprintData.isEmpty()) {
				NBioBSPJNI.FIR_HANDLE hCapturedFIR = nbsp.new FIR_HANDLE();
				NBioBSPJNI.FIR_TEXTENCODE textFIR = nbsp.new FIR_TEXTENCODE();
				NBioBSPJNI.FIR_TEXTENCODE textFIRFromDB = nbsp.new FIR_TEXTENCODE();
				int i = nbsp.OpenDevice();
				System.out.println("i=" + i);
				nbsp.Capture(hCapturedFIR);

				if (nbsp.IsErrorOccured())
					return "NBioBSP Error Occured [" + nbsp.GetErrorCode() + "]";

				NBioBSPJNI.INPUT_FIR inputFIR1 = nbsp.new INPUT_FIR();
				NBioBSPJNI.INPUT_FIR inputFIR2 = nbsp.new INPUT_FIR();
				Boolean result = new Boolean(false);
				nbsp.GetTextFIRFromHandle(hCapturedFIR, textFIR);

				inputFIR1.SetTextFIR(textFIR);
				inputFIR1.SetFIRHandle(hCapturedFIR);
				System.out.println("attendance::" + textFIR.TextFIR);
				NBioBSPJNI.FIR_PAYLOAD payload = nbsp.new FIR_PAYLOAD();

				for (FingerprintData f : fingerprintData) {
					System.out.println("iterating:" + f.getUserId() + " " + f.getFingerPrintData());
					textFIRFromDB.TextFIR = f.getFingerPrintData();
					inputFIR2.SetTextFIR(textFIRFromDB);
					int returnVal = nbsp.VerifyMatch(inputFIR1, inputFIR2, result, payload);

					if (nbsp.IsErrorOccured() == false) {
						if (result) {
							System.out.println("verify ok");
							try {
								boolean checkAttMarked = addAttendanceToForm(mongoCl, f.getUserId());
								if (!checkAttMarked) {
									// status = "<span>Attendance
									// recoded.</span><br/><a
									// href='index.jsp'>Home</a>";
									status = "<script>alert('Attendance marked');window.location.href='http://localhost:8080/Y4J/attendance.jsp';window.location.refresh();</script>";
								} else {
									status = "<script>alert('Attendance for this candidate has been already marked today');window.location.href='http://localhost:8080/Y4J/attendance.jsp';window.location.refresh();</script>";
								}
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {
							System.out.println("verify failed with first data. checking the next in line");
							continue;
						}
					}
				}
				break;
			} else {
				continue;
			}
		}
		if (status.equals("")) {
			return "<span>Fingerprints not matching. Please try again with the fingerprint registered.</span><br/><a href='http://localhost:8080/Y4J/attendance.jsp'>Back</a>";
		}

		nbsp.CloseDevice();
		nbsp.dispose();
		return status;
	}

	/**
	 * Reads the user id from mongo and gets the user's FIR data
	 * 
	 * @param client
	 * @param userId
	 * @return
	 */
	public List<FingerprintData> readFingerPrintData(MongoClient client, String userId) {
		DB db = client.getDB("youthforjobs");
		DBCollection coll = db.getCollection("FIR");
		BasicDBObject whereQuery = new BasicDBObject();
		whereQuery.put("userId", userId);
		DBCursor cursor = coll.find(whereQuery);
		List<FingerprintData> list = new ArrayList<FingerprintData>();
		while (cursor.hasNext()) {
			DBObject obj = cursor.next();
			FingerprintData data = new FingerprintData();
			data.setUserId((String) obj.get("userId"));
			data.setName((String) obj.get("userName"));
			data.setFingerId((Integer) obj.get("fingerId"));
			data.setFingerPrintData((String) obj.get("fingerprintbytes"));

			list.add(data);
		}
		// System.out.println("Size of data from Mongo:" + list.size());
		return list;
	}

	/**
	 * This method take the input and gives the aadhar num and attendance list
	 * of trainee details
	 * 
	 * @param client
	 * @param batchCode
	 * @return
	 */
	public List<String> getTraineeDetails(MongoClient client, String name) {
		DB db = client.getDB("youthforjobs");
		// Get the id for the batch code
		DBCollection batchColl = db.getCollection("forms");
		BasicDBObject batchIdQuery = new BasicDBObject();
		batchIdQuery.put("form.name", name);
		DBCursor cursor1 = batchColl.find(batchIdQuery);

		Object formdata = "";
		List<String> userMap = new ArrayList<String>();
		while (cursor1.hasNext()) {
			DBObject obj = cursor1.next();
			formdata = obj.get("form");
			Users users = new Users();
			// data.setStudents((ArrayList) obj.get("students"));
			System.out.println("formdata::" + formdata);
			Iterator it = ((LinkedHashMap<String, Object>) formdata).entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				if (pair.getKey().equals("aadhar")) {
					users.setAadhar((String) pair.getValue());
				}
			}
			userMap.add(users.getAadhar());
		}

		return userMap;
	}

	/**
	 * Method that updates the attendance for the aadhar number
	 * 
	 * @param client
	 * @param aadhar
	 * @throws ParseException
	 */

	public boolean addAttendanceToForm(MongoClient client, String aadhar) throws ParseException {
		boolean attendanceMarkedToday = false;
		DB db = client.getDB("youthforjobs");
		DBCollection attendance = db.getCollection("forms");
		BasicDBObject whereQuery = new BasicDBObject();
		whereQuery.put("form.aadhar", aadhar);
		DBCursor cursor = attendance.find(whereQuery);
		LinkedHashMap attUpdate = new LinkedHashMap<>();

		ArrayList<String> markAtt = new ArrayList<String>();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		Date today = new Date();
		String attDate = formatter.format(today);
		String markToday = attDate + "-Present";
		Object createdBy = "";
		String userName = "";
		String _v = "";
		String batchCode = "";

		while (cursor.hasNext()) {
			DBObject obj = cursor.next();
			// getting the json object
			createdBy = (Object) obj.get("createdBy");
			userName = (String) obj.get("userName");
			_v = (String) obj.get("_v");
			batchCode = (String) obj.get("batchCode");
			attUpdate = (LinkedHashMap) obj.get("form");
			// iterating form to update the attendance

			Iterator it = attUpdate.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				if (pair.getKey().equals("attendance")) {
					markAtt = (ArrayList) pair.getValue();
					if (markAtt.isEmpty()) {
						markAtt.add(markToday);
						attendanceMarkedToday = false;

					} else {
						// for (String dateFromList : markAtt) {
						SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
						String[] sb = markAtt.get(markAtt.size() - 1).split("-P");
						String[] dbListDate = sb[0].split(" ");
						Date listDate = dateFormat1.parse(dbListDate[0]);
						String[] todayDate = attDate.split(" ");
						Date markingDate = dateFormat1.parse(todayDate[0]);
						if (markingDate.after(listDate)) {
							markAtt.add(markToday);
							attendanceMarkedToday = false;
						}else{
							attendanceMarkedToday = true;
						}
						// }
					}
					break;
				}
			}
			attUpdate.put("attendance", markAtt);
		}
		BasicDBObject docBuilder = new BasicDBObject();
		// writing the json against _id
		docBuilder.put("createdBy", createdBy);
		docBuilder.put("userName", userName);
		docBuilder.put("form", attUpdate);
		docBuilder.put("_v", _v);
		docBuilder.put("batchCode", batchCode);

		WriteResult wr = attendance.update(whereQuery, docBuilder);
		System.out.println("update existing:" + wr.isUpdateOfExisting() + " was ack:" + wr.wasAcknowledged());

		return attendanceMarkedToday;

	}

	/**
	 * Mark attendance in DB
	 * 
	 * @param client
	 * @param userId
	 * @throws ParseException
	 */
	public void markAttendance1(MongoClient client, String userId) throws ParseException {
		DB db = client.getDB("youthforjobs");
		DBCollection attendance = db.getCollection("Attendance");
		BasicDBObject whereQuery = new BasicDBObject();
		whereQuery.put("userId", userId);
		DBCursor cursor = attendance.find(whereQuery);

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		Date today = new Date();

		if (cursor.hasNext()) {
			// System.out.println(cursor.next());
			String[] date = (cursor.next().toString()).split("Date");
			String compareDate = date[1].substring(5, (date[1].lastIndexOf("}") - 1));
			System.out.println(compareDate);
			Date date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(compareDate);
			if (today.after(date1)) {
				System.out.println("after ");
			}
			BasicDBObject docBuilder = new BasicDBObject();
			docBuilder.append("userId", userId);
			docBuilder.append("attendance", "Present");
			docBuilder.append("Date", formatter.format(today));
			attendance.update(whereQuery, docBuilder);
		} else {
			// insert if no record found
			BasicDBObjectBuilder docBuilder = BasicDBObjectBuilder.start();

			docBuilder.append("userId", userId);
			docBuilder.append("attendance", "Present");
			docBuilder.append("Date", formatter.format(today));
			DBObject doc = docBuilder.get();
			WriteResult result = attendance.insert(doc);
		}
		// BObject doc = docBuilder.get();
		// WriteResult result = attendance.insert(doc);

	}

	@GET
	@Path("/getBatches")
	@Produces(MediaType.TEXT_HTML)
	public Response getBatchName() {
		HashMap<String, String> batches = new HashMap<String, String>();

		MongoClient mongoCl = new MongoClient("localhost", 27017);
		DB db = mongoCl.getDB("youthforjobs");
		DBCollection batch = db.getCollection("batches");
		DBCursor cursor = batch.find();

		while (cursor.hasNext()) {
			DBObject obj = cursor.next();
			String batchCode = (String) obj.get("batchCode");
			Object batchId = (Object) obj.get("_id");
			batches.put(batchId.toString(), batchCode);
		}
		
		System.out.println("size:" + batches.size());
		
		StringBuffer options = new StringBuffer();
		options.append("<option value='Select'>Select Batch</option>");
		Iterator it = batches.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry pair = (Map.Entry)it.next();
			options.append("<option value='").append(pair.getKey()).append("'>").append(pair.getValue()).append("</option>");
		}
		return Response.status(200).entity(options.toString()).build();
	}

	@GET
	@Path("/getCandidates/{name}")
	@Produces(MediaType.TEXT_HTML)
	public Response getCandidates(@PathParam("name") String code) {

		String response = "";
		MongoClient mongoCl = new MongoClient("localhost", 27017);
		DB db = mongoCl.getDB("youthforjobs");
		DBCollection forms = db.getCollection("forms");
		BasicDBObject where = new BasicDBObject();
		where.put("batchCode", code);
		DBCursor cursor = forms.find(where);

		ArrayList names = new ArrayList<>();
		while (cursor.hasNext()) {
			DBObject obj = cursor.next();
			Object formdata = obj.get("form");
			Iterator it = ((LinkedHashMap<String, Object>) formdata).entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				if (pair.getKey().equals("name")) {
					names.add((String) pair.getValue());
				}
			}
		}
		StringBuffer options = new StringBuffer();
		options.append("<option value='Select'>Select Name</option>");
		for (int i = 0; i < names.size(); i++) {
			options.append("<option value='").append(names.get(i)).append("'>").append(names.get(i)).append("</option>");
		}
		
		System.out.println(options);
		
		return Response.status(200).entity(options.toString()).build();
	}

}
