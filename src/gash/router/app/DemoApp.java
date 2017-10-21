/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.app;

import java.awt.List;
import java.util.ArrayList;

import gash.router.client.CommConnection;
import gash.router.client.CommListener;
import gash.router.client.MessageClient;
import routing.Pipe.Route;

public class DemoApp implements CommListener {
	private static final int NUM_PINGS = 4; //added by me
	
	private MessageClient mc;
	
	private ArrayList<Long> pingLatencies = new ArrayList<Long>(NUM_PINGS); // added by me
	private ArrayList<Long> postLatencies = new ArrayList<Long>(NUM_PINGS);// added by me

	public DemoApp(MessageClient mc) {
		init(mc);
	}

	private void init(MessageClient mc) {
		this.mc = mc;
		this.mc.addListener(this);
	}

	private void ping(int N) {
		for (int n = 0; n < N; n++) {
			mc.ping();
		}
		
		for (int n = 0; n < N; n++) {
			mc.postMessage("Class Employee class that has ID,"
					+ "Name,Salary hello world from Security Clearance ");
		}
	}

	@Override
	public String getListenerID() {
		return "demo";
	}

	@Override
	public void onMessage(Route msg) {
		// added by me
		
		long latency = (System.currentTimeMillis() - msg.getSendTs());
		
		if (msg.getPath().equals(MessageClient.PING_PATH)) {
			pingLatencies.add(latency);
			System.out.println("Round-trip ping times (msec)");
			System.out.println(pingLatencies.toString());
		} else if (msg.getPath().equals(MessageClient.MESSAGE_PATH)){
			postLatencies.add(latency);
			System.out.println("Round-trip post times (msec)");
			System.out.println(postLatencies.toString());
		}
	}

	/**
	 * sample application (client) use of our messaging service
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String host = "127.0.0.1";
		int port = 4567;

		try {
			MessageClient mc = new MessageClient(host, port);
			DemoApp da = new DemoApp(mc);

			// do stuff w/ the connection
			da.ping(NUM_PINGS);

			System.out.println("\n** exiting in 10 seconds. **");
			System.out.flush();
			Thread.sleep(10000);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			CommConnection.getInstance().release();
		}
	}
}
