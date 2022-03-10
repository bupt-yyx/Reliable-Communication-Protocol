import java.util.StringTokenizer;

public class Message {
    private String type;
    private int seq;
    private int timestamp;
    private int sendtime;
    private String messagecontent;
    public Message (String type,int a,int b,int c,String d)
    {
        this.type=type;
        this.seq=a;
        this.timestamp=b;
        this.sendtime=c;
        this.messagecontent=d;
    }
    public  static String encoder(Message message){
        String str=message.getType()+"+"+message.getSeq()+"+"+message.getSendtime()+"+"+message.getTimestamp()+"+"+message.getMessagecontent();
        return str;
    }
    public static void decoder(String msg,Message message){
        StringTokenizer stringTokenizer=new StringTokenizer(msg,"+");
        message.setType(stringTokenizer.nextToken());
        message.setSeq(Integer.valueOf(stringTokenizer.nextToken()).intValue());
        message.setSendtime(Integer.valueOf(stringTokenizer.nextToken()).intValue());
        message.setTimestamp(Integer.valueOf(stringTokenizer.nextToken()).intValue());
        message.setMessagecontent(stringTokenizer.nextToken());
    }

    public String getType() {
        return type;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getSendtime() {
        return sendtime;
    }

    public int getSeq() {
        return seq;
    }

    public String getMessagecontent() {
        return messagecontent;
    }

    public void setSendtime(int sendtime) {
        this.sendtime = sendtime;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMessagecontent(String messagecontent) {
        this.messagecontent = messagecontent;
    }

}
