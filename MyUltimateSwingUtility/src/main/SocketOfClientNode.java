package main;


import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.List;

public class SocketOfClientNode {
    private Socket socket;
    private final String ip;
    private final int port;
    private RSAPublicKey pubKeyOfServer;
    private SecretKey AESKey;
    private byte[] encryptedAESKey;
    private byte[] initVector;
    private IvParameterSpec initVectorIV;
    public boolean isActive = false;
    static final byte LAST_MARKER = "l".getBytes(StandardCharsets.UTF_8)[0];
    static final byte NAME_BOUND = "=".getBytes(StandardCharsets.UTF_8)[0];
    static final int TIMEOUT = 300000; //Лимит времени чтения данных сегмента из сокета или из потока


    public SocketOfClientNode(String ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        updateRes();
        initSecure();
        isActive = true;
    }

    public void updateRes() throws IOException {
        socket = new Socket(ip, port);
        socket.setSoTimeout(2000);
        InputStream in = socket.getInputStream();
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        out.println("GETRES");
        List<Segment> segments = getAllSegments(in);
        checkCoreDir();
        for (Segment seg : segments) {
            saveCoreRes(seg);
        }
        socket.close();
    }

    private void sendSegment(OutputStream out, Segment seg) throws IOException {
        out.write((seg.name + "=").getBytes(StandardCharsets.UTF_8));
        out.write(prepareData(seg.data));
        if (seg.isLast) out.write((byte) 'l');
        else out.write((byte) '-');
        out.flush();
    }

    private List<Segment> getAllSegments(InputStream in) throws IOException {
        List<Segment> segments = new ArrayList<Segment>();
        Segment seg = new Segment(null, null, false);
        while (!seg.isLast) {
            seg = readSegment(in);
            segments.add(seg);
        }
        return segments;
    }

    private static void saveCoreRes(Segment seg) throws IOException {
        String name;
        name = seg.name;
        File file = new File("coreRes", name);
        file.createNewFile();
        FileOutputStream fileWriter = new FileOutputStream(file);
        fileWriter.write(seg.data);
        fileWriter.close();
    }

    private void checkCoreDir() {
        File dir = new File("coreRes");
        if (!dir.isFile()) dir.delete();
        if (!dir.isDirectory()) dir.mkdir();
    }

    private Segment readSegment(InputStream inputStream) throws IOException {
        String name = readFieldName(inputStream);
        byte[] data = readEncodedData(inputStream);
        byte[] forCheck = justRead(inputStream, 1);
        boolean isLast = forCheck[0] == LAST_MARKER;
        return new Segment(name, data, isLast);

    }

    private static byte[] justRead(InputStream inputStream, int num) throws IOException {
        long start = System.currentTimeMillis();
        boolean check = inputStream instanceof ByteArrayInputStream;
        int counter = 0;
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        while (counter != num) {
            byte[] tempData = new byte[num - counter];
            int numberOfRead = inputStream.read(tempData);
            if (numberOfRead == -1)
                numberOfRead = 0;
            counter += numberOfRead;
            result.write(Arrays.copyOf(tempData, numberOfRead));
            if (check)
                break;
            else if (System.currentTimeMillis() - start > TIMEOUT) {
                throw new IOException("Ошибка при чтении потока.");
            }
        }
        return result.toByteArray();
    }

    private static byte[] readEncodedData(InputStream inputStream) throws IOException {
        byte[] lengthArr = justRead(inputStream, 4);
        long length = 0;
        for (int i = 3; i >= 0; i--) {
            length *= 256;
            length += ((int) lengthArr[i] & 0xff);
        }
        if (length > (1024 * 1024 * 100))
            throw new RuntimeException("Беды с размерам данных!!");
        byte[] result = justRead(inputStream, (int) length);
        return result;
    }

    private static String readFieldName(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (true) {
            byte[] c = justRead(inputStream, 1);
            if (c[0] != NAME_BOUND)
                result.append((char) c[0]);
            else break;
            i++;
            if (i > 100) throw new RuntimeException("So long name of segment!");
        }
        return result.toString();
    }


    void getServerPubKey() throws IOException {
        try {
            initConnection();
            InputStream in = socket.getInputStream();
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            out.print("GETSERVERPUBKEY");
            out.flush();
            Segment seg = readSegment(in);
            byte[] encoded = Base64.getDecoder().decode(new String(seg.data, StandardCharsets.UTF_8));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            pubKeyOfServer = (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    private void generateInitVector() {
        int ivSize = 16;
        byte[] iv = new byte[ivSize];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        initVector = iv;
    }

    void getAESKey() {
        try {
            generateInitVector();
            initVectorIV = new IvParameterSpec(initVector);
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128);
            AESKey = kgen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    void encryptAESKeyForServer() {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, pubKeyOfServer);
            encryptedAESKey = cipher.doFinal(AESKey.getEncoded());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    byte[] encryptDataForServer(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, AESKey, initVectorIV);
            return cipher.doFinal(data);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return null;
    }

    void initSecure() throws IOException {
        getAESKey();
        getServerPubKey();
        encryptAESKeyForServer();
    }

    private void secureTransmit(byte[] inputData) {
        ByteArrayOutputStream protectedData = new ByteArrayOutputStream();
        try {
            protectedData.write(initVector);
            protectedData.write(encryptDataForServer(padTo16(prepareData(inputData))));
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] data = protectedData.toByteArray();
        ByteArrayOutputStream valueToSend = new ByteArrayOutputStream();
        try {
            sendSegment(valueToSend, new Segment("key", encryptedAESKey, false));
            sendSegment(valueToSend, new Segment("data", data, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            out.write("SCRTRANSMIT\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.write(prepareData(valueToSend.toByteArray()));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Message.Addition parseAddition(byte[] dataVal) throws IOException {
        ByteArrayInputStream data = new ByteArrayInputStream(dataVal);
        Segment currSeg;
        String type = "";
        String name = "";
        byte[] dataAdd = null;
        do {
            currSeg = readSegment(data);
            switch (currSeg.name) {
                case "data":
                    dataAdd = currSeg.data;
                    break;
                case "name":
                    name = new String(currSeg.data, StandardCharsets.UTF_8);
                    break;
                case "type":
                    type = new String(currSeg.data, StandardCharsets.UTF_8);
                    break;
            }
        } while (!currSeg.isLast);

        if (!type.equals("image") || dataAdd == null)
            return null;
        else return new Message.Addition(new FilePrototype(name, dataAdd).toImageIcon());
    }

    private Message encodeMessageSegment(Segment seg) throws IOException {
        ByteArrayInputStream data = new ByteArrayInputStream(seg.data);
        Message result = new Message();
        if (seg.data.length == 3 && new String(seg.data, StandardCharsets.UTF_8).equals("END")) {
            result.isNewsEnded = true;
            return result;
        }
        Segment currSeg;
        do {
            currSeg = readSegment(data);
            switch (currSeg.name) {
                case "text":
                    result.text = new String(currSeg.data, StandardCharsets.UTF_8);
                    break;
                case "author":
                    result.author = new Contact(new String(currSeg.data, StandardCharsets.UTF_8));
                    break;
                case "addition":
                    Message.Addition mayBeData = parseAddition(currSeg.data);
                    if (mayBeData != null)
                        result.additions.add(mayBeData);
            }
        } while (!currSeg.isLast);

        return result;
    }

    private byte[] readSecureData(InputStream in) throws IOException {
        try {
            List<Segment> segs = getAllSegments(in);
            Segment res = null;
            Segment hash = null;
            for (Segment seg : segs) {
                if (seg.name.equals("result"))
                    res = seg;
                else if (seg.name.equals("hash"))
                    hash = seg;
            }

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, AESKey, initVectorIV);
            return cipher.doFinal(res.data);
        } catch (IllegalBlockSizeException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
            cryptoErrorAction();
        }
        return null;
    }

    private byte[] padTo16(byte[] inp) {
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        try {
            res.write(inp);
            if (inp.length % 16 != 0) {
                int n = 16 - (inp.length % 16);
                for (int i = 0; i < n; i++) {
                    res.write(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res.toByteArray();
    }

    synchronized public Message getNews(int index) {
        try {
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            sendSegment(request, new Segment("newsRequestByNumber", String.valueOf(index).getBytes(StandardCharsets.UTF_8), true));
            initConnection();
            secureTransmit(request.toByteArray());
            byte[] result = readSecureData(socket.getInputStream());
            closeConnectionAnyway();
            if (result == null)
                return null;
            ByteArrayInputStream data = new ByteArrayInputStream(result);
            Segment news = readSegment(data);
            return encodeMessageSegment(news);
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeConnectionAnyway();
        return null;
    }

    private Segment secureTransmitRequest(Segment request) throws IOException {
        ByteArrayOutputStream requestData = new ByteArrayOutputStream();
        try {
            sendSegment(requestData, request);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initConnection();
        secureTransmit(requestData.toByteArray());
        byte[] result = readSecureData(socket.getInputStream());
        if (result == null)
            return null;
        closeConnectionAnyway();
        return new Segment("result", result, true);
    }


    public List<Contact> getContactList() throws IOException {
        List<Contact> result = new ArrayList<>();
        Segment contactsSeg = secureTransmitRequest(new Segment("getContacts", new byte[0], true));
        if (contactsSeg == null)
            throw new IOException("Беды с передачей секретного запроса");
        if (contactsSeg.data.length == 4 && new String(contactsSeg.data, StandardCharsets.UTF_8).equals("NOPE"))
            return new ArrayList<Contact>();
        ByteArrayInputStream contactsSegData = new ByteArrayInputStream(contactsSeg.data);
        List<Segment> segs = getAllSegments(contactsSegData);
        for (Segment seg : segs) {
            List<Segment> currFields = getAllSegments(new ByteArrayInputStream(seg.data));
            String name = null;
            FilePrototype icon = null;
            FilePrototype maxIcon = null;
            String description = "";
            for (Segment field : currFields) {
                switch (field.name) {
                    case "name":
                        name = new String(field.data, StandardCharsets.UTF_8);
                        break;
                    case "description":
                        description = new String(field.data, StandardCharsets.UTF_8);
                        break;
                    case "iconMin":
                        icon = readFile(new ByteArrayInputStream(field.data));
                        break;
                    case "iconMax":
                        maxIcon = readFile(new ByteArrayInputStream(field.data));
                        break;
                }
            }
            if (name == null)
                throw new IOException("Получен безымянный контакт.");
            Contact currCont = new Contact(name);
            currCont.description = description;
            if (icon != null) currCont.icon = icon.toImageIcon();
            if (maxIcon != null) currCont.maxIcon = maxIcon.toImageIcon();
            result.add(currCont);
        }
        return result;
    }

    public boolean doLogin(String login, String password) throws IOException {
        ByteArrayOutputStream requestData = new ByteArrayOutputStream();
        sendSegment(requestData, new Segment("login", login.getBytes(StandardCharsets.UTF_8), false));
        sendSegment(requestData, new Segment("password", password.getBytes(StandardCharsets.UTF_8), true));
        Segment result = secureTransmitRequest(new Segment("loginRequest", requestData.toByteArray(), true));
        if (result == null) return false;
        return new String(result.data, StandardCharsets.UTF_8).equals("OK");
    }

    public boolean sendMessage(Message message, String login, String password) throws IOException {
        ByteArrayOutputStream requestData = new ByteArrayOutputStream();
        sendSegment(requestData, new Segment("login", login.getBytes(StandardCharsets.UTF_8), false));
        sendSegment(requestData, new Segment("password", password.getBytes(StandardCharsets.UTF_8), false));
        sendSegment(requestData, new Segment("news", messageToSegmentData(message), true));
        Segment result = secureTransmitRequest(new Segment("sendNewsRequest", requestData.toByteArray(), true));
        if (result == null) return false;
        return new String(result.data, StandardCharsets.UTF_8).equals("OK");
    }

    private byte[] messageToSegmentData(Message nwsOrMsg) throws IOException {
        ByteArrayOutputStream resultData = new ByteArrayOutputStream();
        sendSegment(resultData, new Segment("text", nwsOrMsg.text.getBytes(StandardCharsets.UTF_8), false));
        for (Message.Addition addition : nwsOrMsg.additions) {
            Segment seg = additionToSegment(addition);
            if (seg != null)
                sendSegment(resultData, seg);
        }
        sendSegment(resultData, new Segment("author", nwsOrMsg.author.name.getBytes(StandardCharsets.UTF_8), true));
        return resultData.toByteArray();
    }

    private Segment additionToSegment(Message.Addition addition) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        if (addition.type != Message.Addition.TypeOfAddition.IMAGE)
            return null; // TODO: 06.05.2021
        FilePrototype file = FilePrototype.fromImageIcon(addition.valueImage);
        sendSegment(result, new Segment("name", file.name.getBytes(StandardCharsets.UTF_8), false));
        sendSegment(result, new Segment("data", file.data, false));
        sendSegment(result, new Segment("type", "image".getBytes(StandardCharsets.UTF_8), true));
        return new Segment("addition", result.toByteArray(), false);
    }


    private void closeConnectionAnyway() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initConnection() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port), 1000);

    }

    private byte[] prepareData(byte[] data) {
        int length = data.length;
        byte[] lengthValue = new byte[4];
        for (int i = 0; i < 4; i++) {
            lengthValue[i] = (byte) (length % 256);
            length /= 256;
        }
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        try {
            res.write(lengthValue);
            res.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res.toByteArray();
    }

    private FilePrototype readFile(InputStream in) throws IOException {
        if (in.available() == 4 && new String(justRead(in, 4), StandardCharsets.UTF_8).equals("NOPE"))
            return null;
        List<Segment> segments = getAllSegments(in);
        String name = null;
        byte[] data = null;
        for (Segment currSeg : segments) {
            if (currSeg.name.equals("name"))
                name = new String(currSeg.data);
            else if (currSeg.name.equals("data"))
                data = currSeg.data;
        }
        if (data == null || name == null)
            throw new IllegalArgumentException("Беды с файлами.");
        return new FilePrototype(name, data);
    }

    public boolean sendChangeProfileRequest(String login, String password, Contact self) throws IOException {
        ByteArrayOutputStream requestData = new ByteArrayOutputStream();

        sendSegment(requestData, new Segment("password", password.getBytes(StandardCharsets.UTF_8), false));
        sendSegment(requestData, new Segment("login", login.getBytes(StandardCharsets.UTF_8), false));
        if (self.maxIcon != null) {
            sendSegment(requestData, new Segment("iconMax", FileToSegmentData(FilePrototype.fromImageIcon(self.maxIcon)), false));
        }
        // TODO: 07.05.2021
        if (self.icon != null) {
            sendSegment(requestData, new Segment("iconMin", FileToSegmentData(FilePrototype.fromImageIcon(self.icon)), false));
        }
        sendSegment(requestData, new Segment("description", self.description.getBytes(StandardCharsets.UTF_8), true));


        Segment result = secureTransmitRequest(new Segment("updateUserRequest", requestData.toByteArray(), true));
        if (result == null) return false;
        return new String(result.data, StandardCharsets.UTF_8).equals("OK");
    }

    public byte[] FileToSegmentData(FilePrototype file) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        sendSegment(result, new Segment("name", file.name.getBytes(StandardCharsets.UTF_8), false));
        sendSegment(result, new Segment("data", file.data, true));
        return result.toByteArray();
    }

    void cryptoErrorAction() {

    }
}


class Segment {
    String name;
    byte[] data;
    boolean isLast;

    Segment(String name, byte[] data, boolean isLast) {
        this.name = name;
        this.data = data;
        this.isLast = isLast;
    }

    byte[] getDataHashCode() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            return md.digest(this.data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }


}

