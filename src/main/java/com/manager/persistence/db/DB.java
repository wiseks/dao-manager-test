package com.manager.persistence.db;


import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 数据库辅助类
 *
 */
public class DB {

	private static final Log log = LogFactory.getLog(DB.class);

	/*
	 * 数据库连接池
	 */
	private final DataSource dataSource;

	/*
	 * 缓慢查询
	 */
	private int slowLog = 100;

	/*
	 * 名字
	 */
	private final String name;

	/*
	 * 持久化的线程池
	 */
	private ScheduledThreadPoolExecutor[] pools;

	/**
	 * 使用配置文件构造
	 *
	 * @param properties    配置文件
	 */
	public DB(Properties properties) throws Exception{
		this.name = properties.getProperty("db.name");
		this.dataSource = BasicDataSourceFactory.createDataSource(properties);
		if (properties.containsKey("db.slowLog"))
			this.slowLog = Integer.parseInt(properties.getProperty("db.slowLog"));
		if (properties.containsKey("db.saveThreads")) {
			int threads = Integer.parseInt(properties.getProperty("db.saveThreads"));
			pools = new ScheduledThreadPoolExecutor[threads];
			for (int i=0; i<threads; i++) {
				pools[i] = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
					
					@Override
					public Thread newThread(Runnable r) {
						return new Thread();
					}
				});
			}
		}
	}

	public DB(String name, DataSource dataSource, int threads) {
		this.name = name;
		this.dataSource = dataSource;
		this.slowLog = 100;
		if (threads > 0) {
			pools = new ScheduledThreadPoolExecutor[threads];
			for (int i=0; i<threads; i++) {
				pools[i] = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
					
					@Override
					public Thread newThread(Runnable r) {
						return new Thread();
					}
				});
			}
		}
	}

	public String getName() {
		return name;
	}

	public int getSlowLog() {
		return slowLog;
	}

	protected ScheduledExecutorService getPool(int hash) {
		if (pools == null)
			return null;
		return pools[Math.abs(hash)%pools.length];
	}

	/**
	 * 从连接池中获取一条连接
	 *
	 * @return 连接
	 * @throws SQLException 参考{@link DataSource#getConnection()}
	 */
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	/**
	 * 执行更新操作，操作结果参考{@link Statement#executeUpdate(String)}
	 *
	 * @param sql 更新语句
	 * @return 更新语句影响的记录条数
	 * @throws SQLException 数据库异常
	 */
	public int update(String sql) throws SQLException {
		if (log.isDebugEnabled())
			log.debug(sql);
		Connection connection = null;
		Statement statement = null;
		long t = System.currentTimeMillis();
		try {
			connection = dataSource.getConnection();
			statement = connection.createStatement();
			return statement.executeUpdate(sql);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					log.error("close statement failed", e);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					log.error("close connection failed", e);
				}
			}
			t = System.currentTimeMillis() - t;
			if (t > slowLog) {
				if (log.isWarnEnabled()) {
					log.warn("slow sql [" + t + "]" + sql);
				}
			}
		}
	}

	/**
	 * 执行查询操作
	 *
	 * @param sql 查询语句
	 * @return 查询的结果
	 * @throws SQLException 执行数据库操作时的异常
	 */
	public List<Map<String, Object>> query(String sql) throws SQLException {
		if (log.isDebugEnabled())
			log.debug(sql);
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		long t = System.currentTimeMillis();
		try {
			connection = dataSource.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			List<Map<String, Object>> list = new ArrayList<>();
			ResultSetMetaData meta = rs.getMetaData();
			while (rs.next()) {
				Map<String, Object> map = new HashMap<>();
				for (int i = 0; i < meta.getColumnCount(); i++) {
					String name = meta.getColumnLabel(i + 1);
					Object value = rs.getObject(name);
					map.put(name, value);
				}
				list.add(map);
			}
			return list;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					log.error("close result failed", e);
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					log.error("close statement failed", e);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					log.error("close connection failed", e);
				}
			}
			t = System.currentTimeMillis() - t;
			if (t > slowLog) {
				if (log.isWarnEnabled()) {
					log.warn("slow sql [" + t + "]" + sql);
				}
			}
		}
	}

	public void shutdown() {
		if (pools == null)
			return;
		for (ScheduledThreadPoolExecutor pool : pools) {
			try {
				pool.shutdown();
				while(!pool.isTerminated()) {
					if (log.isWarnEnabled())
						log.warn(pool.getQueue().size() + " task(s) in save queue." );
					try {
						pool.awaitTermination(1, TimeUnit.SECONDS);
					} catch (InterruptedException e) {}
				}
			} catch (Throwable t) {
				log.error(t);
			}
		}
	}
}
