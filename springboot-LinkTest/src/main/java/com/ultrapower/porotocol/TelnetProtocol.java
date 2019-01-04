package com.ultrapower.porotocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ultrapower.driver.resdriver.transport.*;
import org.apache.commons.lang.ObjectUtils;

import com.ultrapower.driver.resdriver.common.ErrorCode;
import com.ultrapower.driver.resdriver.keyword.Colon;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class TelnetProtocol extends Transport implements Runnable {
	
    private static final String TERMINAL_TYPE = "vt100";
    private boolean authed = false;

    private String prompt;

    public DataInputStream dis = null;

    public DataOutputStream dos = null;

    private byte[] buffer = new byte[1024 * 100];

    private VirtualTerminal virtualTerminal = new VirtualTerminal();

    private Socket socket;
    
	private StringBuffer buf = new StringBuffer();
	
	private StringBuffer expired = new StringBuffer();
	
	private boolean isPool = false;
    
	private Thread t = null;
	
    /**
     * 资源回显编码
     */
    private String sysEncoding;

    private void ioTest() throws TransportException {
        if (this.dis == null || this.dos == null) {
            throw new TransportException(NET_ERROR);
        }
    }

    public TelnetProtocol() {
    }

    public void ini(String ip, int port, int timeout, int tryTimes,
            String echoTime) throws TransportException {

        this.setIp(ip);
        this.setPort(port);
        this.setTimeout(timeout);
        this.setTryTimes(tryTimes);
        if( (null!=echoTime) && !("".equals(echoTime))){
        	this.setEchoTime(new Integer(echoTime));
        }

        try {
            SocketAddress socketAddress = new InetSocketAddress(ip, port);
            this.socket = new Socket();
            this.socket.connect(socketAddress, getTimeout());
            this.socket.setSoTimeout(getTimeout());
            this.dis = new DataInputStream(this.socket.getInputStream());
            this.dos = new DataOutputStream(this.socket.getOutputStream());
            
            buf.append(recieve());
            
            isPool = true;
            t = new Thread(this);
            t.start();
        } catch (UnknownHostException ex) {
            throw new TransportException(ex);
        } catch (IOException ex) {
            throw new TransportException(ex);
        }
    }

    private void sendAuthData(String userid, String passwd)
            throws TransportException {
        String inputStr = read2ColonEcho();
        System.out.println("-------------userid-----"+userid+"-----------------");
        System.out.println("sendAuthData 1回显："+inputStr);
        this.sendCommand(userid);
        inputStr = read2ColonEcho();
        System.out.println("sendAuthData 2回显："+inputStr);
        this.sendCommand(passwd);
        this.sendCommand("");
    }

    /**
     * authentication
     *
     * @param uid String
     * @param pwd String
     * @throws AuthenticationException
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    public void auth(String userid, String passwd, String prompt)
            throws TransportAuthenticationException, TransportException {

        this.setPrompt(prompt);
        this.ioTest();
        System.out.println("ioTest ok!");
        this.sendAuthData(userid, passwd);
        System.out.println("auth ok!");
        StringBuffer inputStr = new StringBuffer();
        long litmitedTime = System.currentTimeMillis()+10000L;
        while (true) {
            inputStr.append(this.readEcho().trim());
            System.out.println("auth 回显："+inputStr.toString());
            if(inputStr.toString().endsWith("?")){
                this.sendCommand("vt100");
            }
            if (inputStr.toString() !=null 
                    && inputStr.toString().endsWith(this.getPrompt())) {
                this.setAuthed(true);
                System.out.println("auth OK!!!");
                return;
            } else if (Colon.endsWith(inputStr.toString())) {
            	System.out.println("auth failed!!!");
                this.setAuthed(false);
                throw new TransportAuthenticationException("userid[" + userid
                        + "]->telnet[" + this.getIp() + "]:[" + this.getPort()
                        + "] 认证失败");
            }else if(System.currentTimeMillis()>litmitedTime){
            	throw new TransportException("认证超时");
            }
            //休眠一秒
            try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

    }
    
    
    private void sendAuthDataNoneUsername(String userid, String passwd)
			throws TransportException {
		String inputStr = read2ColonEcho();
		this.sendCommand(passwd);
		// inputStr = read2ColonEcho();
		this.sendCommand("");
	}

    /**
     * authentication
     *
     * @param uid String
     * @param pwd String
     * @throws AuthenticationException
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    public void authNoneUsername(String userid, String passwd, String prompt)
            throws TransportAuthenticationException, TransportException {

        this.setPrompt(prompt);
        this.ioTest();

        this.sendAuthDataNoneUsername(userid, passwd);

        StringBuffer inputStr = new StringBuffer();
        while (true) {
            inputStr.append(this.readEcho().trim());
            if (Colon.endsWith(inputStr.toString())) {
                this.setAuthed(false);
                throw new TransportAuthenticationException("userid[" + userid
                        + "]->telnet[" + this.getIp() + "]:[" + this.getPort()
                        + "] 认证失败");
            }
            if(inputStr.toString().endsWith("?")){
                this.sendCommand("vt100");
            }
            if (inputStr.toString().endsWith(this.getPrompt())) {
                this.setAuthed(true);
                return;
            }
        }

    }
    
    /**
     * close
     *
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    public void close() throws TransportException {
    	isPool = false;
    	t.interrupt();
    	
        try {
            if (this.dis != null) {
                this.dis.close();
                this.dis = null;
            }

            if (this.dos != null) {
                this.dos.close();
                this.dos = null;
            }
            if (this.socket != null) {
                this.socket.close();
                this.socket = null;
            }

        } catch (IOException ex) {
            throw new TransportException(ex);
        }

    }

    /**
     * readUntilColon
     *
     * @return String
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    @Deprecated
    public String read2ColonEcho() throws TransportException {
        StringBuffer str = new StringBuffer();
        long limitedTime = System.currentTimeMillis() + this.getTimeout();
        do {
            str.append(this.readEcho().trim());
    //        System.out.println("------------------登录读取回显...");
            if (str.toString().toLowerCase().indexOf("failure") > 0) {
                throw new TransportException("Telnet登录失败，回显信息为:"
                        + str.toString());
            }
                      
            if (System.currentTimeMillis() > limitedTime) {
                throw new TransportException(ErrorCode.USER_LIST);
            }
        } while (!Colon.endsWith(str.toString()));
        return str.toString();
    }

    /**
     * readUntilKeyWord
     *
     * @param keyWord String
     * @return String
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    @Deprecated
    public String read2KeyWordEcho(String keyWord) throws TransportException {
        StringBuffer rv = new StringBuffer();
        long startTime = System.currentTimeMillis();
        
        while (true) {
            String tmp = this.readEcho();
            rv.append(tmp);
//            System.out.println(rv.toString());
            
            // more命令回显(英文)
            //一页显示不完
            if (tmp.trim().endsWith("--More--")) {
            	
                this.sendCommand(" ");
            }
            
            // more命令回显（SOLARIS中文）
            //一页显示不完
    		Matcher endstartMatcher = Pattern.compile("--还有--\\(\\d+%\\)")
    				.matcher(tmp.trim());
    		if (endstartMatcher.find()) {
    			this.sendCommand(" ");
    		}
    		
    		//结束符
            if (tmp.trim().endsWith(keyWord)) {
                break;
            }
            
            if (tmp.trim().endsWith("word:")
                    || tmp.trim().endsWith("word：") 
                    || tmp.indexOf("Illegal password") > -1
                    || tmp.indexOf("Login Failed") > -1) {
                throw new TransportException(tmp, new Exception());
            }
            
            if((System.currentTimeMillis() - startTime) >= this.getTimeout()){
            	appendEcho("read time out: " + keyWord);
            	throw new TransportException("read time out: " + keyWord);
            }
        }
        
        return rv.toString();
    }
    
	public String readEcho(){
		try {
			Thread.sleep(getEchoTime());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String result = ObjectUtils.toString(buf);
		
		if(expired != null){
			result = result.substring(expired.length());
		}
		
		expired.append(result);
		
		return result;
	}
	
	public String waitingForKeywords(String[] keywords, long timeout) throws TransportException{
		for(int i = 0; i < keywords.length; i++){
			keywords[i] = keywords[i].toUpperCase();
		}
		
		boolean isPool = true;
		StringBuffer buf = new StringBuffer();
		long startTime = System.currentTimeMillis();
		
		do{
			buf.append(readEcho());
			
			if((System.currentTimeMillis() - startTime) >= timeout){
				break;
			}
			
			for(int i = keywords.length - 1; i >= 0; i--){
				if(ObjectUtils.toString(buf).toUpperCase().indexOf(keywords[i]) != -1){
					isPool = false;
					break;
				}
			}
		}while(isPool);
		
		return ObjectUtils.toString(buf);
	}
	
	public String readEchoOfEndsWith(String suffex, long timeout) throws TransportException{
		StringBuffer buf = new StringBuffer();
		long startTime = System.currentTimeMillis();
		
		do{
			buf.append(readEcho());
			/**************chang by dengxj 2013-07-02***************/
			/****************当有分页的时候显示多页********************/
			if(buf.toString().trim().endsWith("-- More --")){
			    this.sendCommand(" ");
			}
			
			if((System.currentTimeMillis() - startTime) >= timeout){
				break;
			}
		}while(false == ObjectUtils.toString(buf).trim().endsWith(suffex));
		
		return ObjectUtils.toString(buf).replace("-- More --", "");
	}
	
    /**
     * recieveEcho
     *
     * @return String
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    public String recieve() throws TransportException {
        ArrayList bsList = new ArrayList();
        //System.out.println("------------------读取回显数据流...");
        try {
            int len = dis.read(this.buffer);
            TelnetNegotiationCmdList cmdList = new TelnetNegotiationCmdList();
            for (int i = 0; i < len; i++) {
                char c = toChar(this.buffer[i]);
                if (c == 255) {
                    TelnetNegotiationCmd command = new TelnetNegotiationCmd();
                    if (toChar(this.buffer[i + 2]) == 1
                            || toChar(this.buffer[i + 2]) == 3) {
                        int verb = toChar(this.buffer[i + 1]);
                        if (verb == 250) {

                        } else if (verb == 251) {
                            command.option = (byte) 253;
                        } else if (verb == 252) {
                            command.option = (byte) 251;
                        } else if (verb == 253) {
                            command.option = (byte) 251;
                        } else if (verb == 254) {
                            command.option = (byte) 252;
                        }
                    } else if (toChar(this.buffer[i + 1]) == 251
                            || toChar(this.buffer[i + 1]) == 252) {
                        command.option = (byte) 254;
                    } else if (toChar(this.buffer[i + 1]) == 253
                            || toChar(this.buffer[i + 1]) == 254) {
                        command.option = (byte) 252;
                    }
                    command.value = this.buffer[i + 2];
                    cmdList.add(command);
                    i += 2;
                } else {
                    Byte b = virtualTerminal.process(c);
                    if (b != null) {
                        bsList.add(b);
                    }
                }

            }
            //System.out.println("------------------读取回显数据流完毕");
            this.dos.write(cmdList.getBytes());
        } catch (IOException ioe) {
            throw new TransportException("接受回显异常", ioe);
        }
        int size = bsList.size();
        byte[] bs = new byte[size];
        for (int i = 0; i < size; i++) {
            bs[i] = ((Byte) bsList.get(i)).byteValue();
        }
        try {
            String rv = new String(bs, this.getEncode());
            if (rv != null && rv.trim().endsWith("?")) {
                this.sendCommand(TERMINAL_TYPE);
            }
            appendEcho(rv);
            //System.out.print(rv);
            return rv;
        } catch (UnsupportedEncodingException ex) {
            throw new TransportException("回显编码转换[" + this.getEncode() + "]异常");
        }      

    }

    /**
     * sendCmd
     *
     * @param cmd String
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    public void sendCommand(String cmd) throws TransportException {
        ioTest();
        if (cmd == null) {
            throw new TransportException("送出的命令为空");
        }
        try {
            this.dos.write(cmd.getBytes());
            this.dos.write(this.COMMAND_END.getBytes());
            try {
                Thread.sleep(this.getEchoTime());
            } catch (InterruptedException ex1) {
                throw new TransportException(ex1);
            }
        } catch (IOException ex) {
            throw new TransportException("发送命令异常", ex);
        }
    }
    
    public void sendHWComment(String cmd) throws TransportException {
    	ioTest();
        if (cmd == null) {
            throw new TransportException("送出的命令为空");
        }
        try {
            this.dos.write(cmd.getBytes());
//            this.dos.writeChar(32);
            dos.flush();
//            this.dos.write(this.COMMAND_END.getBytes());
            try {
                Thread.sleep(this.getEchoTime());
            } catch (InterruptedException ex1) {
            }
        } catch (IOException ex) {
            throw new TransportException("发送命令异常", ex);
        }
    }

    private void clearBAry(byte[] bs) {
        for (int i = 0; i < bs.length; i++) {
            bs[i] = 0;
        }
    }
    

    /**
     *
     * @param b byte
     * @return char
     */
    private char toChar(byte b) {
        return (char) (b & 0xff);
    }

    public void login(String uid, String pwd, String prompt, int tryTime)
            throws TransportException {
        this.setPrompt(prompt);
        this.ioTest();

        this.sendAuthData(uid, pwd);
        StringBuffer inputStr = new StringBuffer();
        long now = System.currentTimeMillis();
        long limitedTime = now + tryTime * getTimeout();
        while (true) {
            inputStr.append(this.readEcho().trim());
            if (Colon.endsWith(inputStr.toString())) {
                this.setAuthed(false);
                throw new TransportAuthenticationException("userid[" + uid
                        + "]->telnet[" + this.getIp() + "]:[" + this.getPort()
                        + "] 认证失败");
            }
            if (inputStr.toString().endsWith(this.getPrompt())) {
                this.setAuthed(true);
                return;
            }
            long runTime = System.currentTimeMillis();
            if (runTime > limitedTime)
                throw new TransportAuthenticationException("登陆超时，回显信息为:"
                        + inputStr.toString());
        }
    }

	public boolean isAuthed() {
		return authed;
	}

	public void setAuthed(boolean authed) {
		this.authed = authed;
	}

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public String getSysEncoding() {
		return sysEncoding;
	}

	public void setSysEncoding(String sysEncoding) {
		this.sysEncoding = sysEncoding;
	}

	/**
	 * 如果资源配置了回显编码，return 资源配置的编码
	 * 否则return 默认编码
	 */
	public String getEncode() {
		// TODO Auto-generated method stub
		if(getSysEncoding() != null && getSysEncoding().trim().length() > 0) {
			return getSysEncoding();
		}
		return DEFAULT_ENCODE;
	}
    
    public void authJuniper(String userid, String passwd, String prompt)
            throws TransportAuthenticationException, TransportException {

        this.setPrompt(prompt);
        this.ioTest();
        this.sendAuthData4JP(userid, passwd);
    }
    
    private void sendAuthData4JP(String userid, String passwd)
			throws TransportException {
		String inputStr = read2ColonEcho();
		this.sendCommand(userid);
		// inputStr = read2ColonEcho();
		this.sendCommand(passwd);
		// this.sendCommand("");
	}
    
    public void run() {
		// TODO Auto-generated method stub
    	while(isPool){
    		try {
				String txt = recieve();
				buf.append(txt);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Throwable t = e;
				
				while(t != null){
					buf.append(t.getMessage());
					t = t.getCause();
				}
				
				isPool = false;
				break;
			}
    	}
	}

	public static void main(String[] args) throws TransportException {
    	String prompt = ">";
    	
		TelnetProtocol telnet = new TelnetProtocol();
		telnet.setSysEncoding("GBK");
		telnet.ini("10.251.111.175", 23, 50000, 3, "500");
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String echo = telnet.readEchoOfEndsWith(":");
		System.out.println(echo);
		telnet.sendCommand("administrator");
		echo = telnet.readEchoOfEndsWith(":");
		System.out.println(echo);
		telnet.sendCommand("Ab123456");
		echo = telnet.readEchoOfEndsWith(prompt);
		System.out.println(echo);
		
		telnet.sendCommand("dir");
		echo = telnet.readEcho();
		System.out.println(echo);
		
		telnet.sendCommand("net localgroup");
		echo = telnet.readEcho();
		System.out.println(echo);
		
		telnet.sendCommand("net Users");
		echo = telnet.readEcho();
		System.out.println(echo);
		
		telnet.sendCommand("exit");
		
		telnet.close();
	}
}
