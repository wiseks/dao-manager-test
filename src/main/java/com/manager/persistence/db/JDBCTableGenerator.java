package com.manager.persistence.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.manager.configration.ClassResolverUtil;
import com.manager.persistence.annotation.Table;
import com.yaowan.game.common.util.json.GsonUtil;

/**
 * 数据库仓储
 *
 */
public class JDBCTableGenerator {

	public static final Log log = LogFactory.getLog(JDBCTableGenerator.class);

	protected DB db;

	public JDBCTableGenerator(DB db) {
		this.db = db;
	}

	// @SuppressWarnings("unchecked")
	// protected Class<T> getClassOfEntity() {
	// return (Class<T>) ((ParameterizedType)
	// (getClass().getGenericSuperclass())).getActualTypeArguments()[0];
	// }

	// public CRepository _getAnnotation(Class<?> clazz) {
	// CRepository repository = clazz.getAnnotation(CRepository.class);
	// if (repository != null) {
	// return repository;
	// }
	// if (clazz.getSuperclass() != null) {
	// return _getAnnotation(clazz.getSuperclass());
	// } else {
	// return null;
	// }
	// }
	//
	// protected CRepository getAnnotation() {
	// return _getAnnotation(this.getClass());
	// }

	// protected JDBCRepository() {
	// CRepository repository = getAnnotation();
	// this.db = DBManager.getInstance().get(repository.source());
	// this.meta = TableMeta.parse(getClassOfEntity());
	// this.slowLog = db.getSlowLog();
	// this.async = repository.async();
	// this.pool = db.getPool(getClassOfEntity().getName().hashCode());
	// if (meta.isAutoCreate()) {
	// try {
	// fixTable();
	// } catch (Exception e) {
	// log.error("fix table failed", e);
	// }
	// }
	// }
	
	 private void diffEntityBetweenTable(TableMeta meta, TableMeta orig) throws SQLException {
	        //字段
	        Set<String> mark = new HashSet<>();
	        for (ColumnMeta columnMeta : meta.getColumns()) {
	            mark.add(columnMeta.getName());
	            ColumnMeta column = orig.getColumnMetaByColumnName(columnMeta.getName());
	            //新增
	            if (column == null) {
	                db.update("ALTER TABLE `" + orig.getName() + "` ADD COLUMN " + columnMeta.buildCreateSQL());
	            } else {
	                //列依然存在，但有变动
	                if (!column.isSame(columnMeta)) {
	                    //列依然存在，但有变动
	                    db.update("ALTER TABLE `" + orig.getName() + "` MODIFY COLUMN " + columnMeta.buildCreateSQL());
	                }
	            }
	        }

	        //索引
	        mark.clear();
	        for (IndexMeta indexMeta : meta.getIndexes().values()) {
	            mark.add(indexMeta.getName());
	            IndexMeta index = orig.getIndexes().get(indexMeta.getName());
	            if (index == null) {
	                db.update("ALTER TABLE `" + orig.getName() + "` ADD " + indexMeta.buildCreateSQL());
	            } else {
	                if (!index.isSame(indexMeta)) {
	                    db.update("DROP INDEX `" + indexMeta.getName() + "` on `" + meta.getName() + "`");
	                    db.update("ALTER TABLE `" + meta.getName() + "` ADD " + indexMeta.buildCreateSQL());
	                }
	            }
	        }
	        for (String indexName : orig.getIndexes().keySet()) {
	            if (!mark.contains(indexName)) {
	                db.update("DROP INDEX `" + indexName + "` on `" + orig.getName() + "`");
	            }
	        }
	    }

	private void diffEntityBetweenTableSql(TableMeta meta, TableMeta orig) throws SQLException {
		// 字段
		Set<String> mark = new HashSet<>();
		for (ColumnMeta columnMeta : meta.getColumns()) {
			mark.add(columnMeta.getName());
			ColumnMeta column = orig.getColumnMetaByColumnName(columnMeta.getName());
			// 新增
			if (column == null) {

				db.update("ALTER TABLE `" + orig.getName() + "` ADD COLUMN " + columnMeta.buildCreateSQL());
			} else {
				// 列依然存在，但有变动
				if (!column.isSame(columnMeta)) {
					// 列依然存在，但有变动
					db.update("ALTER TABLE `" + orig.getName() + "` MODIFY COLUMN " + columnMeta.buildCreateSQL());
				}
			}
		}

		// 索引
		mark.clear();
		for (IndexMeta indexMeta : meta.getIndexes().values()) {
			mark.add(indexMeta.getName());
			IndexMeta index = orig.getIndexes().get(indexMeta.getName());
			if (index == null) {
				db.update("ALTER TABLE `" + orig.getName() + "` ADD " + indexMeta.buildCreateSQL());
			} else {
				if (!index.isSame(indexMeta)) {
					db.update("DROP INDEX `" + indexMeta.getName() + "` on `" + meta.getName() + "`");
					db.update("ALTER TABLE `" + meta.getName() + "` ADD " + indexMeta.buildCreateSQL());
				}
			}
		}
		for (String indexName : orig.getIndexes().keySet()) {
			if (!mark.contains(indexName)) {
				db.update("DROP INDEX `" + indexName + "` on `" + orig.getName() + "`");
			}
		}
	}

	private List<String> buildCreateSQL(List<Integer> specialCluster, TableMeta meta) {
		List<String> resultList = new ArrayList<>();
		if (meta.getClusterBy() == null) {
			resultList.add(buildCreateSQL(meta.getName(), meta));
			return resultList;
		}
		for (int i = 0; i < meta.getCluster(); i++) {// 已存在的则表示需要判断性的创建
			if (specialCluster.contains(i)) {
				continue;
			}
			resultList.add(buildCreateSQL(meta.getName() + "_" + i, meta));
		}
		return resultList;
	}
	
	public void fixtTable(String basePackage){
		ClassResolverUtil<Object> util = new ClassResolverUtil<Object>();
		Set<Class<?>> set = util.find(basePackage).getMatches();
		for(Class<?> clazz : set){
			Table table = clazz.getAnnotation(Table.class);
			if(table!=null){
				TableMeta tm = TableMeta.parse(clazz);
				try {
					this.fixTable(clazz, tm);
				} catch (Exception e) {
					log.error("error",e);
					e.printStackTrace();
				}
			}
		}
	}

	private void fixTable(Class<?> clazz,TableMeta meta) throws Exception {
		Table annotation = clazz.getAnnotation(Table.class);
		if (!annotation.autoCreate()) {
			return;
		}
		List<Map<String, Object>> list = db.query("SHOW TABLES");
		Set<String> tables = new HashSet<>();
		for (Map<String, Object> map : list) {
			for (Object v : map.values()) {
				tables.add(v.toString());
			}
		}
		// 分表意味着一对多.所以需要反过来判定.
		List<Integer> specialClusters = new ArrayList<>();
		if (meta.getCluster() > 0) {
			// 分表需要整体调整
//			for (int i = 0; i < meta.getCluster(); i++) {
//				if (tables.contains(meta.getName() + "_" + i)) {
//					TableMeta orig = TableMeta.getTableFromDB(meta.getName() + "_" + i, db);
//					diffEntityBetweenTable(meta, orig);
//					specialClusters.add(i);
//				}
//			}

			if (specialClusters.size() < meta.getCluster()) {
				List<String> sql = buildCreateSQL(specialClusters,meta);
				for (String s : sql) {
					db.update(s);
				}
			}
		} else {
			if (tables.contains(meta.getName())) {
				TableMeta orig = TableMeta.getTableFromDB(meta.getName(), db);
				diffEntityBetweenTable(meta, orig);
			} else {

				List<String> sql = buildCreateSQL(specialClusters,meta);
				for (String s : sql) {
					db.update(s);
				}
			}
		}
	}

	public String buildCreateSQL(String tableName, TableMeta meta) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("`(\n");
		// columns
		for (ColumnMeta columnMeta : meta.getColumns()) {
			sb.append(columnMeta.buildCreateSQL()).append(",");
			sb.append("\n");
		}
		// pk
		sb.append("PRIMARY KEY (`").append(meta.getPk().getName()).append("`)");
		// index
		if (meta.getIndexes().size() > 0) {
			sb.append(",\n");
			int p = 0;
			for (IndexMeta idx : meta.getIndexes().values()) {
				sb.append(idx.buildCreateSQL());
				if (p < meta.getIndexes().size() - 1) {
					sb.append(",\n");
				}
				p++;
			}
		} else {
			sb.append("\n");
		}
		sb.append(")");
		if (!"".equals(meta.getCharset())) {
			sb.append(" DEFAULT CHARACTER SET ").append(meta.getCharset());
		}
		sb.append(" COMMENT='").append(meta.getComment()).append("'\n");
		return sb.toString();
	}

	public String buildInsertSQL(Object entity, TableMeta meta) {
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO `");
		sb.append(meta.getRealName(entity));
		sb.append("` (");
		StringBuilder vTemp = new StringBuilder();
		StringBuilder cTemp = new StringBuilder();
		for (int i = 0; i < meta.getColumns().size(); i++) {
			ColumnMeta col = meta.getColumns().get(i);
			if (!col.isPk() || !col.isAuto()) {
				cTemp.append("`").append(col.getName()).append("`, ");
				vTemp.append("?, ");
			}
		}
		sb.append(cTemp.subSequence(0, cTemp.length() - 2));
		sb.append(") VALUE (").append(vTemp.subSequence(0, vTemp.length() - 2)).append(")");
		return sb.toString();
	}

	public void prepareStatement(PreparedStatement ps, int index, ColumnMeta meta, Object entity)
			throws IllegalAccessException, IllegalArgumentException, SQLException {
		switch (meta.getType()) {
		case INT:
			ps.setInt(index, meta.getField().getInt(entity));
			break;
		case BIGINT:
			ps.setLong(index, meta.getField().getLong(entity));
			break;
		case SMALLINT:
			ps.setShort(index, meta.getField().getShort(entity));
			break;
		case TINYINT:
			if (meta.getField().getType() == Boolean.class || meta.getField().getType() == boolean.class) {
				ps.setByte(index, meta.getField().getBoolean(entity) ? (byte) 1 : 0);
			} else {
				ps.setByte(index, meta.getField().getByte(entity));
			}
			break;
		case DOUBLE:
			ps.setDouble(index, meta.getField().getDouble(entity));
			break;
		case CHAR:
		case VARCHAR:
		case TEXT:
			if (!meta.isNotNull() && (meta.getField().get(entity)) == null) {
				ps.setString(index, null);
			} else {
				ps.setString(index, meta.getField().get(entity).toString());
			}
			break;
		case DATETIME:
			if (!meta.isNotNull() && (meta.getField().get(entity)) == null) {
				ps.setTimestamp(index, null);
			} else {
				ps.setTimestamp(index, new Timestamp(((Date) (meta.getField().get(entity))).getTime()));
			}
			break;
		default:
			ps.setString(index, GsonUtil.beanToJson(meta.getField().get(entity)));
		}
	}

	public void prepareStatementInsert(PreparedStatement ps, Object entity, TableMeta meta) throws Exception {
		int index = 1;
		for (int i = 0; i < meta.getColumns().size(); i++) {
			ColumnMeta col = meta.getColumns().get(i);
			if (!col.isPk() || !col.isAuto()) {
				prepareStatement(ps, index++, col, entity);
			}
		}
	}

	public String buildDeleteSQL(TableMeta meta, Object entity) {
		return "DELETE FROM `" + meta.getRealName(entity) + "` WHERE `" + meta.getPk().getName() + "` = ?";
	}

	public String buildUpdateSQL(Object entity, TableMeta meta) {
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE `").append(meta.getRealName(entity)).append("` SET ");
		for (int i = 0; i < meta.getColumns().size(); i++) {
			ColumnMeta col = meta.getColumns().get(i);
			if (!col.isReadOnly() && !col.isPk()) {
				sb.append("`").append(col.getName()).append("` = ?").append(",");
			}
		}
		String withToken = sb.toString();
		String removeLast = withToken.substring(0, withToken.length() - 1);
		sb = new StringBuilder();
		sb.append(removeLast);
		sb.append(" WHERE `").append(meta.getPk().getName()).append("` = ?");
		return sb.toString();
	}

	public void prepareStatementUpdate(PreparedStatement ps, Object entity, TableMeta meta) throws Exception {
		int index = 1;
		for (ColumnMeta col : meta.getColumns()) {
			if (!col.isReadOnly() && !col.isPk()) {
				prepareStatement(ps, index++, col, entity);
			}
		}
		prepareStatement(ps, index, meta.getPk(), entity);
	}

	public String buildSelectSQL(Object id, TableMeta meta) {
		if (meta.getClusterBy() != null) {
			throw new RuntimeException("can not query cluster table[" + meta.getName() + "] by id");
		}

		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		for (int i = 0; i < meta.getColumns().size(); i++) {
			ColumnMeta col = meta.getColumns().get(i);
			sb.append("`").append(col.getName()).append("`");
			if (i < meta.getColumns().size() - 1) {
				sb.append(",");
			}
		}
		sb.append(" FROM `").append(meta.getName()).append("` WHERE `").append(meta.getPk().getName()).append("` = ")
				.append(meta.getPk().getValue(id));
		return sb.toString();
	}

	public String buildWhereSQL(Map<String, Object> options, TableMeta meta) {
		// 使用索引
		int counter = 0;
		IndexMeta used = null;
		for (IndexMeta i : meta.getIndexes().values()) {
			int c = 0;
			for (String fieldName : options.keySet()) {
				ColumnMeta col = meta.getColumnMetaByFieldName(fieldName);
				if (col == null) {
					throw new RuntimeException(
							"filed[" + fieldName + "] not found in entity meta[" + meta.getName() + "]");
				}
				if (i.getColumns().contains(col.getName())) {
					c++;
				}
			}
			if (c > counter) {
				counter = c;
				used = i;
			}
		}

		// 没有索引或无法使用索引时，乱序生成where语句
		Iterator<String> it;
		if (used == null) {
			it = options.keySet().iterator();
			if (log.isWarnEnabled()) {
				StringBuilder sb = new StringBuilder();
				options.keySet().forEach(n -> sb.append(n).append(","));
				log.warn("no index found when query " + meta.getName() + " with options:" + sb.toString());
			}
		} else {
			List<String> list = new ArrayList<>();
			Set<String> clone = new HashSet<>(options.keySet());
			for (String colName : used.getColumns()) {
				if (options.containsKey(colName)) {
					list.add(colName);
					clone.remove(colName);
				}
			}
			list.addAll(clone);
			it = list.iterator();
		}
		StringBuilder sb = new StringBuilder();
		if (it.hasNext()) {
			sb.append(" WHERE ");
		}
		while (it.hasNext()) {
			String colName = it.next();
			Object value = options.get(colName);
			ColumnMeta col = meta.getColumnMetaByFieldName(colName);
			sb.append("`").append(col.getName()).append("` = ").append(col.getValue(value));
			if (it.hasNext()) {
				sb.append(" AND ");
			}
		}
		return sb.toString();
	}

	public String buildSelectSQL2(Map<String, Object> option, TableMeta meta) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		for (int i = 0; i < meta.getColumns().size(); i++) {
			ColumnMeta col = meta.getColumns().get(i);
			sb.append("`").append(col.getName()).append("`");
			if (i < meta.getColumns().size() - 1) {
				sb.append(",");
			}
		}
		if (meta.getClusterBy() == null) {
			sb.append(" FROM `").append(meta.getName()).append("` ");
		} else {
			Object cluster = option.get(meta.getClusterBy().getField().getName());
			if (cluster == null) {
				throw new RuntimeException("cluster needed when query table");
			}
			sb.append(" FROM `").append(meta.getName()).append("_")
					.append(Math.abs(cluster.hashCode() % meta.getCluster())).append("`");
		}
		if (option != null) {
			sb.append(buildWhereSQL(option, meta));
		}
		return sb.toString();

	}

	// @SuppressWarnings("unchecked")
	// public List<T> parse(TableMeta meta, List<Map<String, Object>> rs)
	// throws SQLException {
	// List<T> list = new ArrayList<>();
	// try {
	// for (Map<String, Object> map : rs) {
	// T t = (T) meta.getClazz().newInstance();
	// for (ColumnMeta col : meta.getColumns()) {
	// if (map.containsKey(col.getName())) {
	// col.cast(t, map.get(col.getName()));
	// }
	// }
	// list.add(t);
	// }
	// } catch (Exception e) {
	// throw new SQLException("build entity from result set failed", e);
	// }
	// return list;
	// }

	// public void _add(T entity) {
	// String sql = buildInsertSQL(entity);
	// if (log.isDebugEnabled()) {
	// log.debug(sql);
	// }
	// Connection connection = null;
	// PreparedStatement ps = null;
	// ResultSet rs = null;
	// long t = System.currentTimeMillis();
	// try {
	// connection = db.getConnection();
	// if (meta.getPk().isAuto()) {
	// ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	// prepareStatementInsert(ps, entity);
	// ps.executeUpdate();
	// rs = ps.getGeneratedKeys();
	// if (rs.next()) {
	// meta.getPk().cast(entity, rs.getLong(1));
	// } else {
	// throw new SQLException("get generated keys from db failed");
	// }
	// } else {
	// ps = connection.prepareStatement(sql);
	// prepareStatementInsert(ps, entity);
	// ps.executeUpdate();
	// }
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// } finally {
	// if (rs != null) {
	// try {
	// rs.close();
	// } catch (SQLException e) {
	// log.error("close result failed", e);
	// }
	// }
	// if (ps != null) {
	// try {
	// ps.close();
	// } catch (SQLException e) {
	// log.error("close statement failed", e);
	// }
	// }
	// if (connection != null) {
	// try {
	// connection.close();
	// } catch (SQLException e) {
	// log.error("close connection failed", e);
	// }
	// }
	// t = System.currentTimeMillis() - t;
	// if (t > slowLog) {
	// if (log.isWarnEnabled()) {
	// log.warn("slow sql [" + t + "]" + sql);
	// }
	// }
	// }
	// }

	// @Override
	// public void add(List<T> entities) {
	// if (async) {
	// pool.schedule(() -> _add(entities), 0, TimeUnit.SECONDS);
	// } else {
	// _add(entities);
	// }
	// }

	// public void _add(List<T> entities) {
	// if (entities == null || entities.size() == 0) {
	// return;
	// }
	// T entity = entities.get(0);
	// String sql = buildInsertSQL(entity);
	//
	// if (log.isDebugEnabled()) {
	// log.debug(sql);
	// }
	// Connection connection = null;
	// PreparedStatement ps = null;
	// long t = System.currentTimeMillis();
	// try {
	// connection = db.getConnection();
	// connection.setAutoCommit(false);
	// ps = connection.prepareStatement(sql);
	// for (T e : entities) {
	// prepareStatementInsert(ps, e);
	// ps.addBatch();
	// }
	// ps.executeBatch();
	// connection.commit();
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// } finally {
	// if (ps != null) {
	// try {
	// ps.close();
	// } catch (SQLException e) {
	// log.error("close statement failed", e);
	// }
	// }
	// if (connection != null) {
	// try {
	// connection.setAutoCommit(true);
	// connection.close();
	// } catch (SQLException e) {
	// log.error("close connection failed", e);
	// }
	// }
	// t = System.currentTimeMillis() - t;
	// if (t > slowLog) {
	// if (log.isWarnEnabled()) {
	// log.warn("slow sql [" + t + "]" + sql);
	// }
	// }
	// }
	// }

	// @Override
	// public void remove(T entity) {
	// if (async) {
	// pool.schedule(() -> _remove(entity), 0, TimeUnit.SECONDS);
	// } else {
	// _remove(entity);
	// }
	// }

	// public void _remove(T entity) {
	// String sql = buildDeleteSQL(meta, entity);
	// if (log.isDebugEnabled()) {
	// log.debug(sql);
	// }
	//
	// Connection connection = null;
	// PreparedStatement ps = null;
	// long t = System.currentTimeMillis();
	// try {
	// connection = db.getConnection();
	// ps = connection.prepareStatement(sql);
	// prepareStatement(ps, 1, meta.getPk(), entity);
	// ps.executeUpdate();
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// } finally {
	// try {
	// if (ps != null) {
	// ps.close();
	// }
	// } catch (SQLException e) {
	// log.error("close statement failed", e);
	// }
	// try {
	// if (connection != null) {
	// connection.close();
	// }
	// } catch (SQLException e) {
	// log.error("close connection failed", e);
	// }
	// t = System.currentTimeMillis() - t;
	// if (t > slowLog) {
	// if (log.isWarnEnabled()) {
	// log.warn("slow sql [" + t + "]" + sql);
	// }
	// }
	// }
	// }

	// @Override
	// public void remove(List<T> entities) {
	// if (async) {
	// pool.schedule(() -> _remove(entities), 0, TimeUnit.SECONDS);
	// } else {
	// _remove(entities);
	// }
	// }

	// public void _remove(List<T> entities) {
	// if (entities == null || entities.size() == 0) {
	// return;
	// }
	// T entity = entities.get(0);
	// String sql = buildDeleteSQL(meta, entity);
	// if (log.isDebugEnabled()) {
	// log.debug(sql);
	// }
	//
	// Connection connection = null;
	// PreparedStatement ps = null;
	// long t = System.currentTimeMillis();
	// try {
	// connection = db.getConnection();
	// connection.setAutoCommit(false);
	// ps = connection.prepareStatement(sql);
	// for (T e : entities) {
	// prepareStatement(ps, 1, meta.getPk(), e);
	// ps.addBatch();
	// }
	// ps.executeBatch();
	// connection.commit();
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// } finally {
	// try {
	// if (ps != null) {
	// ps.close();
	// }
	// } catch (SQLException e) {
	// log.error("close statement failed", e);
	// }
	// try {
	// if (connection != null) {
	// connection.setAutoCommit(true);
	// connection.close();
	// }
	// } catch (SQLException e) {
	// log.error("close connection failed", e);
	// }
	// t = System.currentTimeMillis() - t;
	// if (t > 100) {
	// if (log.isWarnEnabled()) {
	// log.warn("slow sql [" + t + "]" + sql);
	// }
	// }
	// }
	// }
	//
	// @SuppressWarnings("unchecked")
	// @Override
	// public T get(Object id) {
	// String sql = buildSelectSQL(id);
	// try {
	// List<T> list = parse(meta, db.query(sql));
	// if (list == null || list.size() == 0) {
	// return null;
	// }
	// return list.get(0);
	// } catch (SQLException e) {
	// throw new RuntimeException(e);
	// }
	// }
	//
	// @SuppressWarnings("unchecked")
	// @Override
	// public List<T> listAll() {
	// return list(null);
	// }
	//
	// @SuppressWarnings("unchecked")
	// @Override
	// public List<T> list(Map<String, Object> options) {
	// try {
	// String sql = buildSelectSQL2(options);
	// return parse(meta, db.query(sql));
	// } catch (SQLException e) {
	// throw new RuntimeException(e);
	// }
	// }
	//
	// @Override
	// public T get(String field, Object value) {
	// List<T> list = list(field, value);
	// if (list == null || list.size() == 0) {
	// return null;
	// }
	// return list.get(0);
	// }
	//
	// @Override
	// public List<T> list(String field, Object value) {
	// Map<String, Object> options = new HashMap<>();
	// options.put(field, value);
	// return list(options);
	// }
	//
	// @Override
	// public void save(T entity) {
	// if (async) {
	// pool.schedule(() -> _save(entity), 0, TimeUnit.SECONDS);
	// } else {
	// _save(entity);
	// }
	// }
	//
	// public void _save(T entity) {
	// String sql = buildUpdateSQL(entity);
	// if (log.isDebugEnabled()) {
	// log.debug(sql);
	// }
	//
	// Connection connection = null;
	// PreparedStatement ps = null;
	// long t = System.currentTimeMillis();
	// try {
	// connection = db.getConnection();
	// ps = connection.prepareStatement(sql);
	// prepareStatementUpdate(ps, entity);
	// ps.executeUpdate();
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// } finally {
	// try {
	// if (ps != null) {
	// ps.close();
	// }
	// } catch (SQLException e) {
	// log.error("close statement failed", e);
	// }
	// try {
	// if (connection != null) {
	// connection.close();
	// }
	// } catch (SQLException e) {
	// log.error("close connection failed", e);
	// }
	// t = System.currentTimeMillis() - t;
	// if (t > slowLog) {
	// if (log.isWarnEnabled()) {
	// log.warn("slow sql [" + t + "]" + sql);
	// }
	// }
	// }
	// }
	//
	// @Override
	// public void save(List<T> entities) {
	// if (async) {
	// pool.schedule(() -> _save(entities), 0, TimeUnit.SECONDS);
	// } else {
	// _save(entities);
	// }
	// }
	//
	// public void _save(List<T> entities) {
	// if (entities == null || entities.size() == 0) {
	// return;
	// }
	// T entity = entities.get(0);
	// String sql = buildUpdateSQL(entity);
	// if (log.isDebugEnabled()) {
	// log.debug(sql);
	// }
	//
	// Connection connection = null;
	// PreparedStatement ps = null;
	// long t = System.currentTimeMillis();
	// try {
	// connection = db.getConnection();
	// connection.setAutoCommit(false);
	// ps = connection.prepareStatement(sql);
	// for (T e : entities) {
	// prepareStatementUpdate(ps, e);
	// ps.addBatch();
	// }
	// ps.executeBatch();
	// connection.commit();
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// } finally {
	// try {
	// if (ps != null) {
	// ps.close();
	// }
	// } catch (SQLException e) {
	// log.error("close statement failed", e);
	// }
	// try {
	// if (connection != null) {
	// connection.setAutoCommit(true);
	// connection.close();
	// }
	// } catch (SQLException e) {
	// log.error("close connection failed", e);
	// }
	// t = System.currentTimeMillis() - t;
	// if (t > 100) {
	// if (log.isWarnEnabled()) {
	// log.warn("slow sql [" + t + "]" + sql);
	// }
	// }
	// }
	// }
	//
	// @Override
	// @Deprecated
	// public void truncateAll() {
	// List<String> tableNames = meta.getTruncateTableName();
	// String sql = "truncate " + tableNames.get(0);
	// if (log.isDebugEnabled()) {
	// log.debug(sql);
	// }
	// Connection connection = null;
	// PreparedStatement ps = null;
	// long t = System.currentTimeMillis();
	// try {
	// connection = db.getConnection();
	// connection.setAutoCommit(false);
	// ps = connection.prepareStatement(sql);
	// for (String tableName : tableNames) {
	// ps.addBatch("truncate " + tableName);
	//// prepareStatementUpdate(ps, e);
	// ps.addBatch();
	// }
	// ps.executeBatch();
	// connection.commit();
	// } catch (Exception e) {
	// throw new RuntimeException(e);
	// } finally {
	// try {
	// if (ps != null) {
	// ps.close();
	// }
	// } catch (SQLException e) {
	// log.error("close statement failed", e);
	// }
	// try {
	// if (connection != null) {
	// connection.setAutoCommit(true);
	// connection.close();
	// }
	// } catch (SQLException e) {
	// log.error("close connection failed", e);
	// }
	// t = System.currentTimeMillis() - t;
	// if (t > 100) {
	// if (log.isWarnEnabled()) {
	// log.warn("slow sql [" + t + "]" + sql);
	// }
	// }
	// }
	// }

}
