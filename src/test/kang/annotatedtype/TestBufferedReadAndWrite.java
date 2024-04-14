package test.kang.annotatedtype;

import java.io.*;

public class TestBufferedReadAndWrite {

    public static void main(String[] args) {
        copyFile();
    }

    /**
     * 使用缓冲字符输入/输出流，将A磁盘的某文件复制到B磁盘中。
     * <p>
     * throws FileNotFoundException 创建输入流，如果根据路径找不到文件，可能会抛出这个异常
     * throws IOException 写入数据到目标文件中失败，调用 close() 关闭流，可能会抛出这个异常
     */
    private static void copyFile() {
        // 文件信息
        String sourceFilePath = "D:/test/gls.txt";
        String targetFilePath = "D:/test/success/Writer.txt";
        // 输入流/输出流
        Reader reader = null;
        BufferedReader bufferedReader = null;
        Writer writer = null;
        BufferedWriter bufferedWriter = null;

        try {
            // 输入流
            reader = new FileReader(sourceFilePath);
            // 使用默认的缓冲区大小来创建缓冲字符输入流，默认大小为8192个字符
            bufferedReader = new BufferedReader(reader);
            // 输出流
            writer = new FileWriter(targetFilePath);
            bufferedWriter = new BufferedWriter(writer);
            String result = "";
            while ((result = bufferedReader.readLine()) != null) {
                writer.write(result); // 把内容写入文件
                bufferedWriter.newLine(); // 换行 result 是一行数据，所以每写一行就要换行
            }

            writer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) { // new FileWriter();
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        System.out.println("执行完成！");
    }

}

