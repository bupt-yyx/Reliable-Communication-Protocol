import java.io.IOException;
import java.net.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.*;

public class Client {
    private final String IP = "127.0.0.1";
    private final int port = 1111;
    private DatagramSocket socket;  //实现UDP编码的工具包
    private DatagramPacket packet;  //封装数据报的类
    private Scanner scanner;
    private String syn="syn";
    private String ack="ack";
    private String fin="fin";
    private String psh="psh";
    private boolean sendmessage=false;
    private int windowsize=10;
    ConcurrentHashMap<Integer,Message> messagecontainer=new ConcurrentHashMap<>();
    private int key;
    private Message tempmessage;


    final ExecutorService exec = Executors.newFixedThreadPool(1);
    Callable<String> call = new Callable<String>(){
        @Override
        public String call() throws Exception{
            socket.receive(packet);
            return "成功";
        }
    };

    public Client() {
        try {
            socket = new DatagramSocket();  //server 进程和该端口号绑定
            byte[] bytes = new byte[1024];
            packet = new DatagramPacket(bytes, 0, bytes.length);
            scanner = new Scanner(System.in);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    public int timer(){
        Calendar c= Calendar.getInstance();
        int year=c.get(Calendar.YEAR);
        int minute=c.get(Calendar.MINUTE);
        int second=c.get(Calendar.SECOND);
        int time=year*3600+minute*60+second;
//        System.out.println(time);
        return time;
    }



    public void retransmission(String retransmission_messagestr){
        try {
            byte[] bytes1 = retransmission_messagestr.getBytes();
            DatagramPacket retransmission_message= new DatagramPacket(bytes1, 0, bytes1.length,
                    InetAddress.getByName(IP), port);
            socket.send(retransmission_message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean whetherack() throws InterruptedException, ExecutionException {
        String obj = null;
        boolean result=false;
        try {
            Future<String> future = exec.submit(call);
            obj=future.get(2000, TimeUnit.MILLISECONDS);
            String expectack = new String(packet.getData(), 0, packet.getLength());
            if (expectack.equals("ack")) {
                System.out.println("成功接收到服务端ack消息");
                result=true;
            }
        } catch (TimeoutException e) {
            System.out.println("超时");
        }
        return result;
    }


    private String receive(){
        String str1="false";
        try {
            byte[] byte2=new byte[1024];
            DatagramPacket packet1=packet = new DatagramPacket(byte2, 0, byte2.length);
            socket.receive(packet1);
            String str = new String(packet1.getData(), 0, packet1.getLength());
            return str;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str1;
    }

    public void tcpconnect() throws IOException, InterruptedException, TimeoutException, ExecutionException {
        String obj = null;
        byte[] connect = syn.getBytes();
        DatagramPacket sendsyn = new DatagramPacket(connect, 0, connect.length,
                InetAddress.getByName(IP), port);
        socket.send(sendsyn);
        System.out.println("向服务端发送syn消息,开始接收服务端的ack消息");
        if(whetherack()==true){
            byte[] connect3= ack.getBytes();
            DatagramPacket sendack= new DatagramPacket(connect3, 0, connect3.length,
                    InetAddress.getByName(IP), port);
            socket.send(sendack);
            sendmessage=true;
        }
    }
    public void startsendmessage() {
        try {
            int i=1;
            while(sendmessage) {
                System.out.println("输入消息:");
                String s = scanner.nextLine();
                Message message=new Message("psh",i,timer(),1,s);
                String msg=Message.encoder(message);
                byte[] bytes1 = msg.getBytes();
                DatagramPacket sendpacket = new DatagramPacket(bytes1, 0, bytes1.length,
                        InetAddress.getByName(IP), port);
                if(s.equals("exit")){
                    byte[] bytes2 =fin.getBytes();
                    DatagramPacket end= new DatagramPacket(bytes2, 0, bytes2.length,
                            InetAddress.getByName(IP), port);
                    socket.send(end);
                    sendmessage=false;
                    break;
                }
                if(!s.equals("yyx")){
                    socket.send(sendpacket);
                }
                messagecontainer.put(i%windowsize,message);
                i++;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        Client client=new Client();
        client.tcpconnect();
        new Thread(()->{
            try {
                while (client.sendmessage) {
                    int nowtime= client.timer();
//                    System.out.println(nowtime);
                    if(!client.messagecontainer.isEmpty()) {
                        client.messagecontainer.forEach((key, tempmessage) ->
                        {
                            if (nowtime - tempmessage.getTimestamp() > 5 && tempmessage.getSendtime() <= 5) {
                                String tempstr = Message.encoder(tempmessage);
                                client.retransmission(tempstr);
                                System.out.println("第" + tempmessage.getSendtime() + "次重发消息第" + tempmessage.getSeq() + "消息");
                                tempmessage.setSendtime(tempmessage.getSendtime()+1);
                                tempmessage.setTimestamp(nowtime);
                            }
                        });
                    }
                        Thread.sleep(400);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(()->{ ;
            Message message=new Message(null,0,0,0,null);
                    while (client.sendmessage) {
                        if(!client.messagecontainer.isEmpty()){
                            String str=client.receive();
                            Message.decoder(str,message);
                            System.out.println(str);
                            client.messagecontainer.remove(message.getSeq()%10);
                        }
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
        }).start();
        client.startsendmessage();
        System.exit(0);
    }
}
