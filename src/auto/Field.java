package auto;

/**
 * 字段属性
 * 
 * @author chensheng
 */
public class Field {

	/**
	 * 字段名
	 */
	private String fieldName;

	/**
	 * 属性名
	 */
	private String propertyName;

	/**
	 * 字段类型
	 */
	private int dataType;

	/**
	 * 注解
	 */
	private String comment;

	private boolean primaryKey;
	private boolean isAutoIncrement = false;;

	public Field(String fieldName, String propertyName, boolean primaryKey) {
		super();
		this.fieldName = fieldName;
		this.primaryKey = primaryKey;
		this.propertyName = propertyName;
	}

	public String getFieldName() {
		return fieldName;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public boolean isAutoIncrement() {
		return isAutoIncrement;
	}

	public void setAutoIncrement(boolean isAutoIncrement) {
		this.isAutoIncrement = isAutoIncrement;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

}
