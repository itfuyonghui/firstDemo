package com.ultrapower.controller;
import com.ultrapower.driver.exception.ResDriverException;
import com.ultrapower.driver.resdriver.common.arg.OpenArg;
import com.ultrapower.driver.resdriver.gd.operatingsystem.unix.LinuxSSH;
import com.ultrapower.driver.resdriver.netunit.arg.NetUnitDriverArg;
import com.ultrapower.driver.resdriver.operatingsystem.arg.OSDeviceDriverArg;
import com.ultrapower.pojo.CvsObject;
import com.ultrapower.pojo.LinkResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileSystemView;

/**
 * Created by dell on 2018/12/26.
 */
public class ImportCVS {

    /**
     * @param
     */
	/*
	 * 获取csv文件路径
	 */
    public static String getCSVPath(){
        int result = 0;
        File file = null;
        String path = null;
        JFileChooser fileChooser = new JFileChooser();
        FileSystemView fsv = FileSystemView.getFileSystemView();  //注意了，这里重要的一句
        System.out.println(fsv.getHomeDirectory());                //得到桌面路径
        fileChooser.setCurrentDirectory(fsv.getHomeDirectory());
        fileChooser.setDialogTitle("请选择要上传的文件...");
        fileChooser.setApproveButtonText("确定");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        result = fileChooser.showOpenDialog(new JFrame());
        if (JFileChooser.APPROVE_OPTION == result) {
            path=fileChooser.getSelectedFile().getPath();
            System.out.println("path: "+path);
            return path;
        }
        return null;
    }

    /*
     * 根据 CSV文件路径，解析文件，获取所有的ip地址
     */
    public static List<String> readCvs(String path ){
        File csv = new File(path);  // CSV文件路径
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(csv));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String line = "";
        String everyLine = "";
        try {
            List<String> allString = new ArrayList<String>();
            while ((line = br.readLine()) != null)  //读取到的内容给line变量
            {
                everyLine = line;
                System.out.println(everyLine);
                allString.add(everyLine);
            }
            System.out.println("csv表格中所有行数："+allString.size());
            return allString;
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static void writeCvs(List<CvsObject> objs,String path) {
        try {
            String[] dirs = path.split("\\\\");
            int index = dirs.length;
            index--;
            String lastDir = "\\\\"+dirs[index];
            path.replaceAll(lastDir,"");
                    //String txtPath = "C:/Users/admin/Desktop/";
                    //String txtPath = "C:\\Users\\dell\\Desktop\\";
            String filenameTemp;

            filenameTemp = path + "测试设备的连通性结果" + ".csv";
            File filename = new File(filenameTemp);
            if (!filename.exists()) {
                filename.createNewFile();
            }
            writeCvsFile(objs,filenameTemp);

            System.out.println("--------cvs文件已经写入--------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     * 写文件
     *
     * @param
     *
     * @throws IOException
     */
    public static boolean writeCvsFile(List<CvsObject> obj,String filenameTemp) throws IOException {
        // 先读取原有文件内容，然后进行写入操作
        boolean flag = false;
//		String filein = newStr + "\r\n";
        String temp = "";

        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        StringBuffer buf = new StringBuffer();
        buf.append("IP,Port,Protocol,Account,Pwd,SuCmd,SuperPwd,Prompt,SuperPrompt,NetworkResult,AccountResult"+ "\r\n");
        FileOutputStream fos = null;
        PrintWriter pw = null;
        try {
            // 文件路径
            File file = new File(filenameTemp);
            for (CvsObject str : obj) {
                buf.append(str.getIp()+","+str.getPort()+","+str.getProtocol() +"," +str.getAccount()+","+str.getPassWord()+ ","+str.getSuCmd()+","+str.getSuperPassWord()+","+str.getPrompt()+","+str.getSuperPrompt()+","+str.getConnResult()+","+str.getLoginResult()+"\r\n");
            }

            fos = new FileOutputStream(file);
            pw = new PrintWriter(fos);
            pw.write(buf.toString().toCharArray());
            pw.flush();
            flag = true;
        } catch (IOException e1) {
            // TODO 自动生成 catch 块
            throw e1;
        } finally {
            if (pw != null) {
                pw.close();
            }
            if (fos != null) {
                fos.close();
            }
            if (br != null) {
                br.close();
            }
            if (isr != null) {
                isr.close();
            }
            if (fis != null) {
                fis.close();
            }
        }
        return flag;
    }

    public static List<CvsObject> getCvsList(List<String> lines){
        lines.remove(0);

        ArrayList<CvsObject> cvsObjects = new ArrayList<CvsObject>();
        for (String line:lines) {
            line += ",flag";
            String[] elements = line.split(",");
            CvsObject cvsObject = new CvsObject();
//            for(int i=0;i<elements.length;i++){
//                if(elements[i]==null&&"".equals(elements[i])){
//                    elements[i]="111";
//                }
//            }
            cvsObject.setIp(elements[0]);
            cvsObject.setPort(elements[1]);
            cvsObject.setProtocol(elements[2]);
            cvsObject.setAccount(elements[3]);
            cvsObject.setPassWord(elements[4]);
            cvsObject.setSuCmd(elements[5]);
            cvsObject.setSuperPassWord(elements[6]);
            cvsObject.setPrompt(elements[7]);
            cvsObject.setSuperPrompt(elements[8]);
            cvsObjects.add(cvsObject);
        }
        return cvsObjects;
    }

    public static LinkResult LinkDriver(CvsObject cvsObject) {
        if ("Telnet".equals(cvsObject.getProtocol())) {
            CvsTelnet cvsTelnet = new CvsTelnet();
            LinkResult linkResult = cvsTelnet.open(cvsObject);
            return linkResult;
        } else if ("SSH".equals(cvsObject.getProtocol())) {
            CvsSSH ssh = new CvsSSH();
            LinkResult linkResult = ssh.open(cvsObject);
            return linkResult;
        }
        return new LinkResult(false,"未连接");
    }

    public static void main(String[] args) {
        String str = "127.0.0.1,33,,,"+",flag";
        String[] strs = str.split(",");
        for(int i=0;i<strs.length;i++){
            if(strs[i]==null||"".equals(strs[i])){
                strs[i]="null";
            }
        }
        for (int j=0;j<strs.length;j++) {
            System.out.println("arrayIndex:"+j+"--->> value:"+strs[j]);
        }

    }
}
