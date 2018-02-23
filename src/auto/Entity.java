package auto;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 根据数据库表字段自动生成 实体+Mybatis配置
 * 
 * @author chensheng
 */
public class Entity {

	private final String ENTITY = "model";

	private final String MAPPER_CLASS = "mapper";

	private final String SERVICE = "service";

	private final String SRC = "/src/";

	private final String CONFIG = "/resource/mapper/";

	private final String NEWLINE = "\r\n";

	private final Boolean MYSQL = true;

	private boolean deleteFlag = true;

	// 主键属性
	Field primaryKeyField;

	// 其他属性
	ArrayList<Field> fields = new ArrayList<Field>();

	// 文件夹路径
	private String fileDir;

	// 代码内部导入包路径
	private String entityPakage;

	private String mapperClassPakage;

	private String servicePakage;

	// 实体名称
	private String entityName = null;

	private Connection conn;

	private String tableName;

	private String tableComment;

	public Entity(String fileDir, String basePakage) {
		this.fileDir = fileDir;
		this.entityPakage = basePakage + "." + ENTITY;
		this.mapperClassPakage = basePakage + "." + MAPPER_CLASS;
		this.servicePakage = basePakage + "." + SERVICE;
	}

	// 读取数据库配置文件 & 加载数据库连接
	private void connectDB() throws IOException, ClassNotFoundException, SQLException {
		Class.forName(AutoProperties.jdbcdriver);
		conn = DriverManager.getConnection(AutoProperties.jdbcurl, AutoProperties.username, AutoProperties.password);
	}

	/**
	 * 生成JDBC参数
	 */
	public String createProperties(Field field) {
		StringBuffer codeBuffer = new StringBuffer();
		String jdbcType = Convert.getJDBCType(field.getDataType());
		codeBuffer.append("#{");
		if (StringUtils.isNotBlank(jdbcType)) {
			codeBuffer.append(field.getPropertyName() + ",jdbcType=" + Convert.getJDBCType(field.getDataType()));
		} else {
			codeBuffer.append(field.getPropertyName());
		}
		codeBuffer.append("}");

		return codeBuffer.toString();
	}

	/**
	 * 生成Set方法
	 */
	public String createSetter(Field field) {
		StringBuffer codeBuffer = new StringBuffer();

		codeBuffer.append(
				"    public void set" + Convert.getGetSetterNameByProp(field.getPropertyName()) + "(" + Convert.getDataType(field.getDataType()) + " "
						+ field.getPropertyName() + ") {").append(NEWLINE);
		codeBuffer.append("        this." + field.getPropertyName() + " = " + field.getPropertyName() + ";").append(NEWLINE);
		codeBuffer.append("    }").append(NEWLINE).append(NEWLINE);

		return codeBuffer.toString();
	}

	/**
	 * 生成Get方法
	 */
	public String createGetter(Field field) {
		StringBuffer codeBuffer = new StringBuffer();

		codeBuffer.append("    public " + Convert.getDataType(field.getDataType()) + " get" + Convert.getGetSetterNameByProp(field.getPropertyName()) + "() {")
				.append(NEWLINE);
		codeBuffer.append("        return this." + field.getPropertyName() + ";").append(NEWLINE);
		codeBuffer.append("    }").append(NEWLINE).append(NEWLINE);

		return codeBuffer.toString();
	}

	/**
	 * 是否存在删除标示字段
	 */
	private boolean existDeteleField() {
		for (Field f : fields) {
			if (StringUtils.equals(f.getPropertyName(), "isDeleted")) {
				return true;
			}
		}
		return false;
	}

	void initField(String tableName, String entityName, boolean deleteFlag) throws Exception {
		// 开始生成
		connectDB();

		this.tableName = tableName;
		this.entityName = entityName;
		this.deleteFlag = deleteFlag;

		Statement stmt = conn.createStatement();
		ResultSet result = stmt.executeQuery("select * from " + tableName + " where 1 = 2 ");

		ResultSetMetaData metData = result.getMetaData();

		int ColumnCount = metData.getColumnCount();
		DatabaseMetaData databaseMetaData = conn.getMetaData();

		ResultSet priKeySet = databaseMetaData.getPrimaryKeys(null, null, tableName);
		ResultSet columnSet = databaseMetaData.getColumns(null, null, this.tableName, null);

		ResultSet tableSet = null;
		if (MYSQL) {
			tableSet = stmt.executeQuery("select * from information_schema.TABLES where table_name ='" + tableName + "'");
		}

		String primaryKeyFieldName = null;
		String primaryKeyPropertyName = null;

		if (priKeySet.next()) {
			primaryKeyFieldName = priKeySet.getString("COLUMN_NAME");
			primaryKeyPropertyName = Convert.getJavaBeanPropsNameBy(primaryKeyFieldName);
			primaryKeyField = new Field(primaryKeyFieldName, primaryKeyPropertyName, true);
		}
		priKeySet.close();

		for (int i = 1; i <= ColumnCount; i++) {
			String fieldName = metData.getColumnName(i);
			int iColumnType = metData.getColumnType(i);

			if (!fieldName.equalsIgnoreCase(primaryKeyFieldName)) {
				String propertyName = Convert.getJavaBeanPropsNameBy(fieldName);
				Field field = new Field(fieldName, propertyName, false);
				field.setDataType(iColumnType);
				fields.add(field);
			} else {
				if (null != primaryKeyField) {
					primaryKeyField.setDataType(iColumnType);
					primaryKeyField.setAutoIncrement(metData.isAutoIncrement(i));
				}
			}
		}

		// 获取字段注解
		while (columnSet.next()) {
			String comment = columnSet.getString("REMARKS");
			String column = columnSet.getString("COLUMN_NAME");

			if (StringUtils.isNotBlank(comment)) {
				for (Field field : fields) {
					if (StringUtils.equals(column, field.getFieldName())) {
						field.setComment(comment);
						break;
					}
				}
			}
		}
		columnSet.close();

		// 表注解
		if (tableSet != null && tableSet.next()) {
			tableComment = tableSet.getString("TABLE_COMMENT");
		}
		tableSet.close();

		result.close();
		stmt.close();
		conn.close();
	}

	void builderEntity() throws Exception {
		StringBuffer codeBuffer = new StringBuffer();
		codeBuffer.append("package " + entityPakage + ";").append(NEWLINE).append(NEWLINE);

		codeBuffer.append("import java.io.Serializable;").append(NEWLINE);
		codeBuffer.append("import java.util.Date;").append(NEWLINE).append(NEWLINE);

		if (StringUtils.isNotBlank(tableComment)) {
			codeBuffer.append("/**").append(NEWLINE);
			codeBuffer.append(" * " + tableComment + "实体对象").append(NEWLINE);
			codeBuffer.append(" **/").append(NEWLINE);
		}

		codeBuffer.append("public class " + this.entityName + " implements Serializable {").append(NEWLINE).append(NEWLINE);

		codeBuffer.append("    private static final long serialVersionUID = 1L;").append(NEWLINE).append(NEWLINE);

		if (this.primaryKeyField != null) {
			if (StringUtils.isNotBlank(primaryKeyField.getComment())) {
				codeBuffer.append("    /**").append(NEWLINE);
				codeBuffer.append("     * " + primaryKeyField.getComment()).append(NEWLINE);
				codeBuffer.append("     **/").append(NEWLINE);
			}
			codeBuffer.append("    private " + Convert.getDataType(this.primaryKeyField.getDataType()) + " " + this.primaryKeyField.getPropertyName() + ";")
					.append(NEWLINE).append(NEWLINE);
		}

		for (Field field : fields) {
			if (StringUtils.isNotBlank(field.getComment())) {
				codeBuffer.append("    /**").append(NEWLINE);
				codeBuffer.append("     * " + field.getComment()).append(NEWLINE);
				codeBuffer.append("     **/").append(NEWLINE);
			}
			codeBuffer.append("    private " + Convert.getDataType(field.getDataType()) + " " + field.getPropertyName() + ";").append(NEWLINE).append(NEWLINE);
		}

		codeBuffer.append("    ").append(NEWLINE);
		codeBuffer.append("    public " + this.entityName + "() {").append(NEWLINE);
		codeBuffer.append("    }").append(NEWLINE);

		// 主键的getter和setter方法
		if (this.primaryKeyField != null) {
			codeBuffer.append(createSetter(this.primaryKeyField));
			codeBuffer.append(createGetter(this.primaryKeyField));
		}

		for (Field field : fields) {
			codeBuffer.append(createSetter(field));
			codeBuffer.append(createGetter(field));
		}
		codeBuffer.append("}");

		// 生成文件
		String fileName = this.fileDir + SRC + entityPakage.replaceAll("\\.", "\\/") + "/" + this.entityName + ".java";

		System.out.println(fileName);

		File file = new File(fileName);

		try {
			FileUtils.writeStringToFile(file, codeBuffer.toString(), "utf-8");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void buildMapper() {
		StringBuffer buff = new StringBuffer();

		String mapper = this.mapperClassPakage + "." + this.entityName + "Mapper";
		String entity = this.entityPakage + "." + this.entityName;

		buff.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(NEWLINE);
		buff.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >").append(NEWLINE);

		buff.append("<mapper namespace=\"" + mapper + "\">").append(NEWLINE).append(NEWLINE);

		// BaseResultMap
		buff.append("	<resultMap id=\"BaseResultMap\" type=\"" + entity + "\">").append(NEWLINE);
		if (primaryKeyField != null) {
			buff.append(
					"		<id column=\"" + primaryKeyField.getFieldName() + "\" property=\"" + primaryKeyField.getPropertyName() + "\" jdbcType=\""
							+ Convert.getJDBCType(primaryKeyField.getDataType()) + "\" />").append(NEWLINE);
		}

		for (Field f : fields) {
			buff.append(
					"		<result column=\"" + f.getFieldName() + "\" property=\"" + f.getPropertyName() + "\" jdbcType=\"" + Convert.getJDBCType(f.getDataType())
							+ "\" />").append(NEWLINE);
		}
		buff.append("	</resultMap>").append(NEWLINE).append(NEWLINE);

		// Base_Column_List
		buff.append("	<sql id=\"Base_Column_List\">").append(NEWLINE);
		buff.append("		");
		if (primaryKeyField != null) {
			buff.append(primaryKeyField.getFieldName() + ",");
		}
		int i = 0;
		for (Field f : fields) {
			if (i == 0) {
				buff.append(f.getFieldName());
			} else {
				buff.append("," + f.getFieldName());
			}
			i++;
		}
		buff.append(NEWLINE);
		buff.append("	</sql>").append(NEWLINE).append(NEWLINE);

		// Alias_Column_List
		buff.append("	<sql id=\"Alias_Column_List\">").append(NEWLINE);
		buff.append("		");
		if (primaryKeyField != null) {
			buff.append("t." + primaryKeyField.getFieldName() + ",");
		}
		i = 0;
		for (Field f : fields) {
			if (i == 0) {
				buff.append("t." + f.getFieldName());
			} else {
				buff.append(",t." + f.getFieldName());
			}
			i++;
		}
		buff.append(NEWLINE);
		buff.append("	</sql>").append(NEWLINE).append(NEWLINE);

		// Base_Condition
		buff.append("	<sql id=\"Base_Condition\">").append(NEWLINE);
		buff.append("		<where>").append(NEWLINE);

		if (existDeteleField()) {
			buff.append("		   t.is_deleted = 0").append(NEWLINE);
		}

		for (Field f : fields) {
			String attrName = f.getPropertyName();
			String dataType = Convert.getDataType(f.getDataType());
			if ((!StringUtils.equals(attrName, "isDeleted") && !StringUtils.equals(attrName, "createId") && !StringUtils.equals(attrName, "updateId"))
					&& (StringUtils.equals(dataType, "String") || StringUtils.equals(dataType, "Integer") || StringUtils.equals(dataType, "Long") || StringUtils
							.equals(dataType, "Double"))) {
				buff.append("		   <if test=\"" + attrName + " != null\">and t." + f.getFieldName() + " = " + createProperties(f) + "</if>").append(NEWLINE);
			}
		}
		buff.append("		   <!-- 自定义 -->").append(NEWLINE);
		buff.append("		</where>").append(NEWLINE);
		buff.append("	</sql>").append(NEWLINE).append(NEWLINE);

		// select
		buff.append("	<!-- 通过复合条件查询 -->").append(NEWLINE);
		buff.append("	<select id=\"select\" resultMap=\"BaseResultMap\" parameterType=\"java.util.HashMap\">").append(NEWLINE);
		buff.append("		select").append(NEWLINE);
		buff.append("		<include refid=\"Alias_Column_List\" />").append(NEWLINE);
		buff.append("		from " + this.tableName + " t").append(NEWLINE);
		buff.append("		<include refid=\"Base_Condition\" />").append(NEWLINE);
		buff.append("	</select>").append(NEWLINE);
		buff.append(NEWLINE);

		// count
		buff.append("	<!-- 统计 -->").append(NEWLINE);
		buff.append("	<select id=\"count\" resultType=\"java.lang.Integer\" parameterType=\"java.util.HashMap\">").append(NEWLINE);
		buff.append("		select count(0)").append(NEWLINE);
		buff.append("		from " + this.tableName + " t").append(NEWLINE);
		buff.append("		<include refid=\"Base_Condition\" />").append(NEWLINE);
		buff.append("	</select>").append(NEWLINE);
		buff.append(NEWLINE);

		// selectById
		if (primaryKeyField != null) {
			buff.append("	<!-- 通过主键查询对象 -->").append(NEWLINE);
			buff.append(
					"	<select id=\"selectById\" resultMap=\"BaseResultMap\" parameterType=\"" + Convert.getFullDataType(primaryKeyField.getDataType()) + "\">")
					.append(NEWLINE);
			buff.append("		select").append(NEWLINE);
			buff.append("		<include refid=\"Base_Column_List\" />").append(NEWLINE);
			buff.append("		from " + this.tableName).append(NEWLINE);
			buff.append("		where " + primaryKeyField.getFieldName() + " = " + createProperties(primaryKeyField));
			if (existDeteleField()) {
				buff.append(" and is_deleted = 0").append(NEWLINE);
			} else {
				buff.append(NEWLINE);
			}
			buff.append("	</select>").append(NEWLINE);
			buff.append(NEWLINE);
		}

		// deleteById
		if (primaryKeyField != null) {
			buff.append("	<!-- 通过主键删除对象 -->").append(NEWLINE);
			if (deleteFlag) {
				buff.append("	<delete id=\"deleteById\" parameterType=\"" + Convert.getFullDataType(primaryKeyField.getDataType()) + "\">").append(NEWLINE);
				buff.append("		delete from " + this.tableName).append(NEWLINE);
				buff.append("		where " + primaryKeyField.getFieldName() + " = " + createProperties(primaryKeyField)).append(NEWLINE);
				buff.append("	</delete>").append(NEWLINE);
			} else {
				if (existDeteleField()) {
					buff.append("	<update id=\"deleteById\" parameterType=\"" + Convert.getFullDataType(primaryKeyField.getDataType()) + "\">").append(NEWLINE);
					buff.append("		update " + this.tableName + " set is_deleted = 1").append(NEWLINE);
					buff.append("		where " + primaryKeyField.getFieldName() + " = " + createProperties(primaryKeyField) + " and is_deleted = 0")
							.append(NEWLINE);
					buff.append("	</update>").append(NEWLINE);
				}
			}
			buff.append(NEWLINE);
		}

		// insertAllField
		buff.append("	<!-- 新增对象(所有字段) -->").append(NEWLINE);
		buff.append("	<insert id=\"insertAllField\" parameterType=\"" + entity + "\">").append(NEWLINE);

		if (primaryKeyField != null && primaryKeyField.isAutoIncrement()) {
			buff.append(
					"		<selectKey resultType=\"" + Convert.getFullDataType(primaryKeyField.getDataType()) + "\" order=\"AFTER\" keyProperty=\""
							+ primaryKeyField.getPropertyName() + "\">").append(NEWLINE);
			buff.append("			SELECT LAST_INSERT_ID() AS " + primaryKeyField.getFieldName()).append(NEWLINE);
			buff.append("		</selectKey>").append(NEWLINE).append(NEWLINE);
		}

		buff.append("		insert into " + this.tableName + "(").append(NEWLINE);
		buff.append("			<include refid=\"Base_Column_List\" />").append(NEWLINE);
		buff.append("		)").append(NEWLINE);
		buff.append("		values(").append(NEWLINE);
		if (primaryKeyField != null) {
			buff.append("			" + createProperties(primaryKeyField) + ",").append(NEWLINE);
		}
		i = 0;
		for (Field f : fields) {
			i++;
			if (i == fields.size()) {
				buff.append("			" + createProperties(f)).append(NEWLINE);
			} else {
				buff.append("			" + createProperties(f) + ",").append(NEWLINE);
			}

		}
		buff.append("		)").append(NEWLINE);
		buff.append("	</insert>").append(NEWLINE).append(NEWLINE);

		// insert
		buff.append("	<!-- 新增对象(部分字段) -->").append(NEWLINE);
		buff.append("	<insert id=\"insert\" parameterType=\"" + entity + "\">").append(NEWLINE);

		if (primaryKeyField != null && primaryKeyField.isAutoIncrement()) {
			buff.append(
					"		<selectKey resultType=\"" + Convert.getFullDataType(primaryKeyField.getDataType()) + "\" order=\"AFTER\" keyProperty=\""
							+ primaryKeyField.getPropertyName() + "\">").append(NEWLINE);
			buff.append("			SELECT LAST_INSERT_ID() AS " + primaryKeyField.getFieldName()).append(NEWLINE);
			buff.append("		</selectKey>").append(NEWLINE).append(NEWLINE);
		}

		buff.append("		insert into " + this.tableName).append(NEWLINE);
		buff.append("		<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">").append(NEWLINE);
		if (primaryKeyField != null) {
			buff.append("			<if test=\"" + primaryKeyField.getPropertyName() + " != null\">" + primaryKeyField.getFieldName() + ",</if>").append(NEWLINE);
		}
		for (Field f : fields) {
			buff.append("			<if test=\"" + f.getPropertyName() + " != null\">" + f.getFieldName() + ",</if>").append(NEWLINE);
		}
		buff.append("		</trim>").append(NEWLINE);

		buff.append("		<trim prefix=\"values (\" suffix=\")\" suffixOverrides=\",\">").append(NEWLINE);
		if (primaryKeyField != null) {
			buff.append("			<if test=\"" + primaryKeyField.getPropertyName() + " != null\">" + primaryKeyField.getFieldName() + ",</if>").append(NEWLINE);
		}
		for (Field f : fields) {
			buff.append("			<if test=\"" + f.getPropertyName() + " != null\">" + createProperties(f) + ",</if>").append(NEWLINE);
		}
		buff.append("		</trim>").append(NEWLINE);
		buff.append("	</insert>").append(NEWLINE).append(NEWLINE);

		// update
		if (primaryKeyField != null) {
			buff.append("	<!-- 修改对象 (部分字段)-->").append(NEWLINE);
			buff.append("	<update id=\"update\" parameterType=\"" + entity + "\">").append(NEWLINE);
			buff.append("		update " + this.tableName).append(NEWLINE);
			buff.append("		<set>").append(NEWLINE);
			for (Field f : fields) {
				buff.append("			<if test=\"" + f.getPropertyName() + " != null\">" + f.getFieldName() + " = " + createProperties(f) + ",</if>").append(NEWLINE);
			}
			buff.append("		</set>").append(NEWLINE);
			buff.append("		where " + primaryKeyField.getFieldName() + " = " + createProperties(primaryKeyField)).append(NEWLINE);
			buff.append("	</update>").append(NEWLINE);
			buff.append(NEWLINE);
		}

		// updateAll
		if (primaryKeyField != null) {
			buff.append("	<!-- 修改对象(所有字段) -->").append(NEWLINE);
			buff.append("	<update id=\"updateAllField\" parameterType=\"" + entity + "\">").append(NEWLINE);
			buff.append("		update " + this.tableName).append(NEWLINE);
			buff.append("		set").append(NEWLINE);

			i = 0;
			for (Field f : fields) {
				i++;
				if (i == fields.size()) {
					buff.append("			" + f.getFieldName() + " = " + createProperties(f)).append(NEWLINE);
				} else {
					buff.append("			" + f.getFieldName() + " = " + createProperties(f) + ",").append(NEWLINE);
				}
			}
			buff.append("		where " + primaryKeyField.getFieldName() + " = " + createProperties(primaryKeyField)).append(NEWLINE);
			buff.append("	</update>").append(NEWLINE);
			buff.append(NEWLINE);
		}

		// batchdInsert
		if (primaryKeyField != null) {
			buff.append("	<!-- 批量插入(所有字段) -->").append(NEWLINE);
			buff.append("	<insert id=\"batchInsert\" parameterType=\"java.util.List\">").append(NEWLINE);
			buff.append("		insert into " + this.tableName + "(").append(NEWLINE);
			buff.append("			<include refid=\"Base_Column_List\" />").append(NEWLINE);
			buff.append("		)").append(NEWLINE);
			buff.append("		values").append(NEWLINE);
			if (primaryKeyField != null) {
				buff.append("		<foreach collection=\"list\" index=\"index\" item=\"item\" separator=\",\">").append(NEWLINE);
				buff.append("		(").append(NEWLINE);
				buff.append("			#{item." + primaryKeyField.getPropertyName() + "}").append(NEWLINE);
				for (Field f : fields) {
					buff.append("			")
							.append("<choose><when test=\"item." + f.getPropertyName() + " != null\">" + ",#{item." + f.getPropertyName() + "}"
									+ "</when><otherwise>,default</otherwise></choose>").append(NEWLINE);
				}
				buff.append("		)").append(NEWLINE);
				buff.append("		</foreach>").append(NEWLINE);
			}
			buff.append("	</insert>").append(NEWLINE).append(NEWLINE);
		}

		// batchdInsertOrUpdate
		if (primaryKeyField != null) {
			buff.append("	<!-- 批量插入或更新(所有字段) -->").append(NEWLINE);
			buff.append("	<update id=\"batchInsertOrUpdate\" parameterType=\"java.util.List\">").append(NEWLINE);
			buff.append("		insert into " + this.tableName + "(").append(NEWLINE);
			buff.append("			<include refid=\"Base_Column_List\" />").append(NEWLINE);
			buff.append("		)").append(NEWLINE);
			buff.append("		values").append(NEWLINE);
			if (primaryKeyField != null) {
				buff.append("		<foreach collection=\"list\" index=\"index\" item=\"item\" separator=\",\">").append(NEWLINE);
				buff.append("		(").append(NEWLINE);
				buff.append("			#{item." + primaryKeyField.getPropertyName() + "}").append(NEWLINE);
				for (Field f : fields) {
					buff.append("			")
							.append("<choose><when test=\"item." + f.getPropertyName() + " != null\">" + ",#{item." + f.getPropertyName() + "}"
									+ "</when><otherwise>,default</otherwise></choose>").append(NEWLINE);
				}
				buff.append("		)").append(NEWLINE);
				buff.append("		</foreach>").append(NEWLINE);
			}
			buff.append("		on duplicate key update");
			int index = 0;
			for (Field f : fields) {
				if (index != 0) {
					buff.append(",");
				}
				buff.append(" " + f.getFieldName() + " = " + "values(" + f.getFieldName() + ")");
				index++;
			}
			buff.append(NEWLINE);
			buff.append("	</update>").append(NEWLINE).append(NEWLINE);
		}

		// batchdDelete
		if (primaryKeyField != null) {
			buff.append("	<!-- 批量删除 -->").append(NEWLINE);
			if (deleteFlag) {
				buff.append("	<delete id=\"batchDelete\" parameterType=\"java.util.List\">").append(NEWLINE);
				buff.append("		delete from " + this.tableName + " where " + primaryKeyField.getFieldName() + " in ").append(NEWLINE);
				buff.append("		<foreach collection=\"list\" index=\"index\" item=\"item\" open=\"(\" separator=\",\" close=\")\">").append(NEWLINE);
				buff.append("			#{item}").append(NEWLINE);
				buff.append("		</foreach>").append(NEWLINE);
				buff.append("	</delete>").append(NEWLINE).append(NEWLINE);
			} else {
				if (existDeteleField()) {
					buff.append("	<update id=\"batchDelete\" parameterType=\"java.util.List\">").append(NEWLINE);
					buff.append("		update " + this.tableName + " set is_deleted = 1 where " + primaryKeyField.getFieldName() + " in ").append(NEWLINE);
					buff.append("		<foreach collection=\"list\" index=\"index\" item=\"item\" open=\"(\" separator=\",\" close=\")\">").append(NEWLINE);
					buff.append("			#{item}").append(NEWLINE);
					buff.append("		</foreach>").append(NEWLINE);
					buff.append("		and is_deleted = 0").append(NEWLINE);
					buff.append("	</update>").append(NEWLINE).append(NEWLINE);
				}
			}

		}

		buff.append("	<!-- 自定义 -->").append(NEWLINE);

		buff.append("</mapper>").append(NEWLINE);

		// 生成文件
		String fileName = this.fileDir + CONFIG + this.entityName + "Mapper.xml";

		File file = new File(fileName);

		System.out.println(fileName);

		try {
			FileUtils.writeStringToFile(file, buff.toString(), "utf-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void buildMapperClass() {
		StringBuffer buff = new StringBuffer();
		buff.append("package " + mapperClassPakage + ";").append(NEWLINE).append(NEWLINE);
		buff.append("import com.feiniu.mallfront.framework.dao.BaseMapper;").append(NEWLINE).append(NEWLINE);
		buff.append("import " + entityPakage + "." + entityName + ";").append(NEWLINE).append(NEWLINE);

		if (StringUtils.isNotBlank(tableComment)) {
			buff.append("/**").append(NEWLINE);
			buff.append(" * " + tableComment + "Mapper对象").append(NEWLINE);
			buff.append(" **/").append(NEWLINE);
		}

		buff.append("public interface " + entityName + "Mapper extends BaseMapper<" + entityName + "> {").append(NEWLINE).append(NEWLINE);

		buff.append("}");

		// 生成文件
		String fileName = this.fileDir + SRC + mapperClassPakage.replaceAll("\\.", "\\/") + "/" + this.entityName + "Mapper.java";
		File file = new File(fileName);

		System.out.println(fileName);

		try {
			FileUtils.writeStringToFile(file, buff.toString(), "utf-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void buildService() {
		StringBuffer buff = new StringBuffer();
		buff.append("package " + servicePakage + ";").append(NEWLINE).append(NEWLINE);
		buff.append("import org.springframework.beans.factory.annotation.Autowired;").append(NEWLINE);
		buff.append("import org.springframework.stereotype.Service;").append(NEWLINE).append(NEWLINE);

		buff.append("import com.feiniu.mallfront.framework.service.BaseService;").append(NEWLINE);
		buff.append("import " + entityPakage + "." + entityName + ";").append(NEWLINE);
		buff.append("import " + mapperClassPakage + "." + entityName + "Mapper;").append(NEWLINE).append(NEWLINE);

		if (StringUtils.isNotBlank(tableComment)) {
			buff.append("/**").append(NEWLINE);
			buff.append(" * " + tableComment + "Service对象").append(NEWLINE);
			buff.append(" **/").append(NEWLINE);
		}

		buff.append("@Service").append(NEWLINE);
		buff.append("public class " + entityName + "Service extends BaseService<" + entityName + ", " + entityName + "Mapper> {").append(NEWLINE)
				.append(NEWLINE);
		buff.append("	@Autowired").append(NEWLINE);
		buff.append("	public void setMapper(" + entityName + "Mapper mapper) {").append(NEWLINE);
		buff.append("		this.mapper = mapper;").append(NEWLINE);
		buff.append("	}").append(NEWLINE).append(NEWLINE);
		buff.append("}").append(NEWLINE);

		// 生成文件
		String fileName = this.fileDir + SRC + servicePakage.replaceAll("\\.", "\\/") + "/" + this.entityName + "Service.java";
		File file = new File(fileName);

		System.out.println(fileName);

		try {
			FileUtils.writeStringToFile(file, buff.toString(), "utf-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
