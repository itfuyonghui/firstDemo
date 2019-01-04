package com.ultrapower;

import com.ultrapower.controller.ImportCVS;
import com.ultrapower.pojo.CvsObject;
import com.ultrapower.pojo.LinkResult;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by dell on 2018/12/26.
 */

@SpringBootApplication
public class MyApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        System.out.println("请输入文件路径(例：C:\\Users\\dell\\Desktop\\test.csv):");
        Scanner scanner = new Scanner(System.in);
        ArrayList<CvsObject> cvsObjects = new ArrayList<CvsObject>();
        String path = scanner.nextLine();
        List<CvsObject> cvsList = null;
        if(path != null && !"".equals(path)){
            List<String> lines = ImportCVS.readCvs(path);
            cvsList = ImportCVS.getCvsList(lines);
        }
        if(cvsList != null){
            for (CvsObject obj:cvsList
                    ) {
                LinkResult linkResult = ImportCVS.LinkDriver(obj);
                if(linkResult.isResult() == false){
                    if("连接失败".equals(linkResult.getMessage())){
                        obj.setConnResult("Connection failed");
                        obj.setLoginResult("Login failed");
                    }else{
                        obj.setConnResult("Connection succeeded");
                        obj.setLoginResult("Login failed");
                    }
                }else{
                    obj.setConnResult("connection succeeded");
                    obj.setLoginResult("login successful");
                }
                cvsObjects.add(obj);
            }
        }
        if(cvsObjects != null){
            ImportCVS.writeCvs(cvsObjects,path);
        }



    }


    protected SpringApplicationBuilder configure(
            SpringApplicationBuilder builder) {
        return builder.sources(this.getClass());
    }


}
