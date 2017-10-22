package gash.router.redis;

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

public class RedisCluster {
	
	private static RedisCluster instance = new RedisCluster();
	
	private JedisCluster jc;
	
	public static JedisCluster getJedisCluster() {
		return instance.jc;
	}
	
	private RedisCluster() {
		Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
		//Jedis Cluster will attempt to discover cluster nodes automatically
		jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7000));
		jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7001));
		jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7002));
		jc = new JedisCluster(jedisClusterNodes);
	}

}
