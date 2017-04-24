package com.y4j.services;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * Class that connects to DB and return the connection handle
 * @author Preeti
 *
 */
public class MongoConnect {

	public static MongoClient connectToDB(){
		ServerAddress sr = new ServerAddress("ds025762.mlab.com", 25762);
		MongoCredential mCred = MongoCredential.createCredential("admin", "landseers", "admin".toCharArray());
		List auth = new ArrayList();
		auth.add(mCred);
		MongoClient mongoCl = new MongoClient(sr, auth);

		return mongoCl;
	}
}
