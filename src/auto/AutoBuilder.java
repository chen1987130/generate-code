package auto;

/**
 * 自动代码生成
 * 
 * @author chensheng
 * @version 2015-08-25
 */
public class AutoBuilder {

	// 源文件所在工程目录
	static String FILE_DIR = "D:\\gen";

	static String PAKAGE_PATH = "com.feiniu.quartz.page";

	// 根据表自行定义
	static String[] TABLE_NAME = new String[] { "qrtz_log" };

	static String[] ENTITY_NAME = new String[] { "QrtzLog" };

	// true:物理删除 false:逻辑删除
	static boolean DELETE_FLAG = true;

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < TABLE_NAME.length; i++) {
			Entity entity = new Entity(FILE_DIR, PAKAGE_PATH);
			entity.initField(TABLE_NAME[i], ENTITY_NAME[i], DELETE_FLAG);
			entity.builderEntity();
			entity.buildMapper();
			entity.buildMapperClass();
			entity.buildService();
		}
	}
}
