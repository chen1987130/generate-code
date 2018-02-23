package auto;

import java.sql.Types;

/**
 * 转换帮助类
 * 
 * @author chensheng
 *
 */
public class Convert {

	public Convert() {

	}

	/**
	 * 数据库命名转Java命名
	 */
	public static String getJavaBeanNameBy(String entityName) {
		String javaBeanName = "";
		entityName = entityName.toLowerCase();
		String[] attrs = entityName.split("_");
		for (int i = 0; i < attrs.length; i++) {
			if (i >= 1) {
				javaBeanName += attrs[i].substring(0, 1).toUpperCase() + attrs[i].substring(1, attrs[i].length());
			}
		}
		return javaBeanName;
	}

	public static String getGetSetterNameByProp(String propName) {
		String getSetterName = propName.substring(0, 1).toUpperCase();
		getSetterName += propName.substring(1);
		return getSetterName;
	}

	public static String getJavaBeanPropsNameBy(String columnName) {
		String javaBeanName = "";
		boolean bFlag = false;
		boolean bFirstFlag = false;
		char ch;

		columnName = columnName.toUpperCase();
		for (int i = 0; i < columnName.length(); i++) {
			ch = columnName.charAt(i);
			if (!bFirstFlag) {
				javaBeanName += Character.toString(columnName.charAt(i)).toLowerCase();
				bFirstFlag = true;
				bFlag = true;
				continue;
			}

			if (!bFlag) {
				javaBeanName += ch;
				bFlag = true;
			} else {
				if (ch != '_') {
					javaBeanName += Character.toString(columnName.charAt(i)).toLowerCase();
				} else {
					bFlag = false;
				}
			}
		}

		return javaBeanName;
	}

	/**
	 * 数据库类型转Java类型
	 */
	public static String getDataType(int iDataType) {
		String dataType = "";
		if (iDataType == Types.VARCHAR || iDataType == Types.CHAR || iDataType == Types.LONGVARCHAR || iDataType == Types.CLOB) {
			dataType = "String";
		} else if (iDataType == Types.INTEGER || iDataType == Types.BIT || iDataType == Types.TINYINT || iDataType == Types.SMALLINT
				|| iDataType == Types.SMALLINT) {
			dataType = "Integer";
		} else if (iDataType == Types.BIGINT) {
			dataType = "Long";
		} else if (iDataType == Types.DOUBLE || iDataType == Types.FLOAT || iDataType == Types.REAL) {
			dataType = "Double";
		} else if (iDataType == Types.DECIMAL || iDataType == Types.NUMERIC) {
			dataType = "BigDecimal";
		} else if (iDataType == Types.DATE || iDataType == Types.TIMESTAMP || iDataType == Types.TIME) {
			dataType = "Date";
		} else if (iDataType == Types.BLOB || iDataType == Types.BINARY || iDataType == Types.VARBINARY || iDataType == Types.LONGVARBINARY) {
			dataType = "byte[]";
		}
		return dataType;
	}

	/**
	 * 数据库类型转Java类型
	 */
	public static String getFullDataType(int iDataType) {
		String dataType = "";
		if (iDataType == Types.VARCHAR || iDataType == Types.NVARCHAR || iDataType == Types.CHAR || iDataType == Types.LONGVARCHAR || iDataType == Types.CLOB) {
			dataType = "java.lang.String";
		} else if (iDataType == Types.INTEGER || iDataType == Types.BIT || iDataType == Types.TINYINT || iDataType == Types.SMALLINT
				|| iDataType == Types.SMALLINT) {
			dataType = "java.lang.Integer";
		} else if (iDataType == Types.BIGINT) {
			dataType = "java.lang.Long";
		} else if (iDataType == Types.DOUBLE || iDataType == Types.FLOAT || iDataType == Types.REAL) {
			dataType = "java.lang.Double";
		} else if (iDataType == Types.DECIMAL || iDataType == Types.NUMERIC) {
			dataType = "java.math.BigDecimal";
		} else if (iDataType == Types.DATE || iDataType == Types.TIMESTAMP || iDataType == Types.TIME) {
			dataType = "java.util.Date";
		} else if (iDataType == Types.BLOB || iDataType == Types.BINARY || iDataType == Types.VARBINARY || iDataType == Types.LONGVARBINARY) {
			dataType = "java.lang.Byte";
		}
		return dataType;
	}

	/**
	 * 数据库类型转JDBC类型
	 */
	public static String getJDBCType(int iDataType) {
		String jdbcType = "";
		if (iDataType == Types.VARCHAR) {
			jdbcType = "VARCHAR";
		} else if (iDataType == Types.NVARCHAR) {
			jdbcType = "NVARCHAR";
		} else if (iDataType == Types.CHAR) {
			jdbcType = "CHAR";
		} else if (iDataType == Types.LONGVARCHAR) {
			jdbcType = "LONGVARCHAR";
		} else if (iDataType == Types.CLOB) {
			jdbcType = "CLOB";
		} else if (iDataType == Types.INTEGER) {
			jdbcType = "INTEGER";
		} else if (iDataType == Types.BIT) {
			jdbcType = "BIT";
		} else if (iDataType == Types.TINYINT) {
			jdbcType = "TINYINT";
		} else if (iDataType == Types.SMALLINT) {
			jdbcType = "SMALLINT";
		} else if (iDataType == Types.BIGINT) {
			jdbcType = "BIGINT";
		} else if (iDataType == Types.DOUBLE) {
			jdbcType = "DOUBLE";
		} else if (iDataType == Types.DECIMAL) {
			jdbcType = "DECIMAL";
		} else if (iDataType == Types.FLOAT) {
			jdbcType = "FLOAT";
		} else if (iDataType == Types.REAL) {
			jdbcType = "REAL";
		} else if (iDataType == Types.NUMERIC) {
			jdbcType = "NUMERIC";
		} else if (iDataType == Types.DATE) {
			jdbcType = "DATE";
		} else if (iDataType == Types.TIMESTAMP) {
			jdbcType = "TIMESTAMP";
		} else if (iDataType == Types.TIME) {
			jdbcType = "TIME";
		} else if (iDataType == Types.BLOB) {
			jdbcType = "BLOB";
		} else if (iDataType == Types.BINARY) {
			jdbcType = "BINARY";
		} else if (iDataType == Types.VARBINARY) {
			jdbcType = "VARBINARY";
		} else if (iDataType == Types.LONGVARBINARY) {
			jdbcType = "LONGVARBINARY";
		}
		return jdbcType;
	}
}
