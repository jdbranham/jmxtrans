package com.googlecode.jmxtrans.example;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.commons.pool.KeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.model.Server;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;

import static java.util.Collections.emptyMap;

/**
 * Walks a JMX tree and prints out all of the unique typenames and their
 * attributes.
 * 
 * This is a good test of the core engine of JmxTrans to ensure that it covers
 * all cases.
 * 
 * @author jon
 */
public class TreeWalker3 {

	private static final Logger log = LoggerFactory.getLogger(TreeWalker3.class);

	/** */
	public static void main(String[] args) throws Exception {
		Server server = new Server("w2", "1105");

		JMXConnector conn = null;
		try {
			conn = JmxUtils.getServerConnection(server);
			MBeanServerConnection mbeanServer = conn.getMBeanServerConnection();

			TreeWalker3 tw = new TreeWalker3();
			tw.walkTree(mbeanServer);
		} catch (IOException e) {
			log.error("Problem processing queries for server: " + server.getHost() + ":" + server.getPort(), e);
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	}

	/** */
	public void walkTree(MBeanServerConnection connection) throws Exception {

		// key here is null, null returns everything!
		Set<ObjectName> mbeans = connection.queryNames(null, null);

		Map<String, String> output = new HashMap<String, String>();

		for (ObjectName name : mbeans) {
			MBeanInfo info = connection.getMBeanInfo(name);
			MBeanAttributeInfo[] attrs = info.getAttributes();

			Query query = new Query();
			query.setObj(name.getCanonicalName());
			ResultCapture resultCapture = new ResultCapture();
			query.addOutputWriter(resultCapture);

			for (MBeanAttributeInfo attrInfo : attrs) {
				query.addAttr(attrInfo.getName());
			}

			try {
				JmxUtils.processQuery(connection, query);
			} catch (AttributeNotFoundException anfe) {
				log.error("Error", anfe);
			}

			for (Result result : resultCapture.results) {
				output.put(result.getTypeName(), query.getAttr().toString());
			}
		}

		for (Entry<String, String> entry : output.entrySet()) {
			log.debug(entry.getKey());
			log.debug(entry.getValue());
			log.debug("-----------------------------------------");
		}
	}

	private static final class ResultCapture implements OutputWriter {

		private List<Result> results;

		@Override
		public void start() throws LifecycleException {}

		@Override
		public void stop() throws LifecycleException {}

		@Override
		public void doWrite(Query query, List<Result> results) throws Exception {
			this.results = results;
		}

		@Override
		public Map<String, Object> getSettings() {
			return emptyMap();
		}

		@Override
		public void setSettings(Map<String, Object> settings) {}

		@Override
		public void validateSetup(Query query) throws ValidationException {}

		@Override
		public void setObjectPoolMap(Map<String, KeyedObjectPool> poolMap) {}
	}
}
