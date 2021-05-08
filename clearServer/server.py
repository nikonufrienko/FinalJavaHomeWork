import hashlib
import os
import json
import socket
from Crypto.Cipher import AES
from Crypto.PublicKey import RSA as RSAC
from Crypto import Random

from M2Crypto import BIO, RSA

commands = ["GETRES", "GETSERVERPUBKEY"]


def parseInt(data):
    result = 0
    dataL = list(data)
    for i in range(3, -1, -1):
        result *= 256
        result += dataL[i]
    return result


print("preparing keys...")
new_key = RSAC.generate(2048, Random.new().read)
public_key = new_key.publickey().exportKey("PEM")
private_key = new_key.exportKey("PEM")
publicKeyToSend = public_key.replace(b'-----BEGIN PUBLIC KEY-----\n', b'').replace(b'\n-----END PUBLIC KEY-----', b'')
print("DONE")
while b'\n' in publicKeyToSend: publicKeyToSend = publicKeyToSend.replace(b'\n', b'')
print(public_key)


def prepare(value):
    # x x x x - длинна
    length_val = len(value)
    arr = []
    for i in range(4):
        arr.append(length_val % 256)
        length_val //= 256
    return bytes(arr) + value


def loadRes(name):
    answer = name.encode() + '='.encode()
    f = open(name, "rb")
    answer += prepare(f.read())
    f.close()
    return answer


def get_resources():
    answer = b''
    answer += loadRes("serverDescription.txt") + b'-'
    answer += loadRes("icon.jpg") + b'l'
    return answer


def get_key():
    return b'pubkey=' + prepare(publicKeyToSend) + b'l'


def perform_command(cmd):
    global commands
    if cmd == "GETRES":
        return get_resources()
    elif cmd == "GETSERVERPUBKEY":
        return get_key()


class Segment:
    name = ''
    data = b''
    isLast = False

    def __init__(self, name, data, isLast):
        self.isLast = isLast
        self.data = data
        self.name = name

    def getBinary(self):
        last = b'-'
        if self.isLast:
            last = b'l'
        return self.name.encode('utf-8') + b'=' + prepare(self.data) + last


def parse_segment(data):
    name = ''
    data_value = b''
    isLast = False
    i = 0
    while data[i:i + 1] != b'=' and i < len(data):
        i += 1
    name = data[:i].decode()
    data[i + 1:i + 1 + 4], len(data[i + 1:i + 1 + 4])
    length_value = parseInt(data[i + 1:i + 1 + 4])
    data_value = data[i + 1 + 4:i + 1 + 4 + length_value]
    isLast = data[i + 1 + 4 + length_value:i + 1 + 4 + length_value + 1] == b'l'
    return [i + 1 + 4 + length_value + 1, Segment(name, data_value, isLast)]


def pars_segments(data):
    internal_data = bytes(list(data).copy())
    res = []
    while True:
        i, seg = parse_segment(internal_data)
        res.append(seg)
        internal_data = internal_data[i:]
        if seg.isLast:
            break
    return res


def pad(plain_text):
    block_size = 16
    number_of_bytes_to_pad = block_size - len(plain_text) % block_size
    ascii_string = bytes([number_of_bytes_to_pad])
    padding_str = number_of_bytes_to_pad * ascii_string
    padded_plain_text = plain_text + padding_str
    return padded_plain_text


class File:
    data = None
    name = None

    def __init__(self, name=None, data=None):
        self.data = data
        self.name = name

    def to_segments(self):
        if self.name is None or self.data is None:
            return b'NOPE'
        result = b''
        result += Segment("name", self.name.encode('utf-8'), False).getBinary()
        result += Segment("data", self.data, True).getBinary()
        return result

    def from_segment_data(self, data_inc):
        print("размер иконки:", len(data_inc))
        segs_inc = pars_segments(data_inc)
        print(len(segs_inc))
        for seg in segs_inc:
            if seg.name == "data":
                self.data = seg.data
            elif seg.name == "name":
                print("Было имя")
                self.name = seg.data.decode('utf-8')
        return self


class Contact:
    def __init__(self, name=None, password=None, description="", iconMax=File(None, None), iconMin=File(None, None)):
        self.iconMin = iconMin
        self.iconMax = iconMax
        self.name = name
        self.description = description
        self.password = password

    def by_name(self, name):
        if not os.path.isdir("contacts/" + name):
            return None
        self.name = name
        f = open("contacts/" + name + "/info.json", "rt")
        dict_info = json.loads(f.read())
        f.close()
        self.password = dict_info["password"]
        self.description = dict_info["description"]
        icon_max_path = dict_info["iconMax"]
        icon_min_path = dict_info["iconMin"]
        if icon_max_path != None:
            f = open("contacts/" + name + "/" + icon_max_path, 'rb')
            self.iconMax = File(dict_info["iconMax"], f.read())
            f.close()
        if icon_min_path != None:
            f = open("contacts/" + name + "/" + icon_min_path, 'rb')
            self.iconMin = File(dict_info["iconMin"], f.read())
            f.close()
        return self

    def to_json(self):
        res_dict = dict()
        res_dict["name"] = self.name
        res_dict["iconMax"] = self.iconMax.name
        res_dict["iconMin"] = self.iconMin.name
        res_dict["password"] = self.password
        res_dict["description"] = self.description
        return json.dumps(res_dict)

    def from_login(self, login, password):
        if login == '' or password == '':
            return None
        if os.path.isdir("contacts/" + login):
            print("loging")
            f = open("contacts/" + login + "/info.json", "rt")
            dict_info = json.loads(f.read())
            f.close()
            if password == dict_info["password"]:
                print(dict_info['password'], password)
                return self.by_name(login)
            else:
                return None
        else:
            os.makedirs("contacts/" + login)
            self.name = login
            self.password = password
            self.description = ""
            f = open("contacts/" + login + "/info.json", "wt")
            f.write(self.to_json())
            f.close()
            value = "[]"
            if os.path.isfile('contactsList.json'):
                f = open('contactsList.json', 'rb')
                value = f.read()
                f.close()
            f = open('contactsList.json', 'wt')
            f.write(json.dumps(json.loads(value) + [self.name]))
            f.close()
        os.sync()

    def update(self):
        if self.name == None or self.password == None:
            print("error: Why are you trying update this?")
            return None
        if self.iconMin.name != None:
            f = open("contacts/" + self.name + '/' + self.iconMin.name, 'wb')
            f.write(self.iconMin.data)
            f.close()
        if self.iconMax.name != None:
            f = open("contacts/" + self.name + '/' + self.iconMax.name, 'wb')
            f.write(self.iconMax.data)
            f.close()
        f = open("contacts/" + self.name + "/info.json", "wt")
        f.write(self.to_json())
        f.close()
        os.sync()

    def toSegment(self):
        result = b''
        result += Segment("iconMin", self.iconMin.to_segments(), False).getBinary()
        result += Segment("iconMax", self.iconMax.to_segments(), False).getBinary()
        result += Segment("name", self.name.encode('utf-8'), False).getBinary()
        result += Segment("description", self.description.encode('utf-8'), True).getBinary()
        return result


class Addition:
    def __init__(self, type_add=None, value=File(None, None)):
        self.value = value
        self.type_add = type_add


class Message:

    def __init__(self, contact_name="unknown", text="", additions_list=None):
        if additions_list is None:
            additions_list = []
        self.additions_list = additions_list
        self.text = text
        self.contact_name = contact_name

    def save_as_news(self):
        ind = 0
        os.sync()
        if not os.path.isdir("news/msg0"):
            os.makedirs("news/msg0")
            f = open("news/counter.counter", "wt")
            f.write("1")
            f.close()
        else:
            f = open("news/counter.counter", "rt")
            ind = int(f.read())
            f.close()
            f = open("news/" + "counter.counter", "wt")
            f.write(str(ind + 1))
            f.close()
            os.makedirs("news/msg" + str(ind))
        res_dict = dict()
        names_of_additions = []
        for add in self.additions_list:
            names_of_additions.append([add.value.name, add.type_add])
        res_dict["additions"] = names_of_additions
        res_dict["author"] = self.contact_name
        res_dict["text"] = self.text
        f = open("news/msg" + str(ind) + "/info.json", "wt")
        f.write(json.dumps(res_dict))
        for add in self.additions_list:
            f = open("news/msg" + str(ind) + "/" + add.value.name, "wb")
            f.write(add.value.data)
            f.close()

    def get_as_news_by_index(self, ind):
        '''
        Файловые дополнения будут возвращатся в виде ссылки (path).
        '''
        if not os.path.isdir("news/msg" + str(ind)): return None
        f = open("news/msg" + str(ind) + "/info.json", "rt")
        json_value = f.read()
        f.close()
        res_dict = json.loads(json_value)
        self.contact_name = res_dict["author"]
        self.text = res_dict["text"]
        names_of_additions = res_dict["additions"]
        self.additions_list = []
        for add in names_of_additions:
            if add[1] == 'image':
                f = open("news/msg" + str(ind) + "/" + add[0], 'rb')
                self.additions_list.append(Addition("image", File(add[0], f.read())))
                f.close()
            else:
                self.additions_list.append(
                    Addition(add[1], File(add[0], ("news/msg" + str(ind) + "/" + add[0]).encode('utf-8'))))
        return self

    def to_segment_data(self):
        result = b''
        result += Segment("text", self.text.encode('utf-8'), False).getBinary()
        for add in self.additions_list:
            result += Segment("addition",
                              Segment('name', add.value.name.encode('utf-8'), False).getBinary()
                              + Segment('data', add.value.data, False).getBinary()
                              + Segment('type', add.type_add.encode('utf-8'), True).getBinary(), False).getBinary()
        result += Segment("author", self.contact_name.encode('utf-8'), True).getBinary()
        return result

    def from_segment_data(self, data):
        segments = pars_segments(data)
        for seg in segments:
            if seg.name == 'text':
                self.text = seg.data.decode('utf-8')
            elif seg.name == 'author':
                self.contact_name = seg.data.decode('utf-8')
            elif seg.name == 'addition':
                addition_segments = pars_segments(seg.data)
                addition = Addition()
                for additions_segment in addition_segments:
                    if additions_segment.name == "name":
                        addition.value.name = additions_segment.data.decode("utf-8")
                    elif additions_segment.name == "data":
                        addition.value.data = additions_segment.data
                    elif additions_segment.name == "type":
                        addition.type_add = additions_segment.data.decode("utf-8")
                self.additions_list.append(addition)
        return self


def num_of_news():
    if not os.path.isfile('news/counter.counter'):
        return -1
    else:
        f = open('news/counter.counter', 'rt')
        result = int(f.read())
        f.close()
        return result


def get_all_contacts_as_segment_data():
    if not os.path.isfile('contactsList.json'):
        f = open('contactsList.json', 'wt')
        f.write(json.dumps([]))
        f.close()
    f = open('contactsList.json', 'rt')
    cnts = json.loads(f.read())
    f.close()
    result = b''
    for i in range(len(cnts)):
        result += Segment('contact', Contact().by_name(cnts[i]).toSegment(), i == len(cnts) - 1).getBinary()
    return result


def get_news_by_num(num):
    news_data = Message().get_as_news_by_index(num_of_news() - num - 1)
    if news_data is None:
        news_data = b'END'
    else:
        news_data = news_data.to_segment_data()
    return Segment("news", news_data, True).getBinary()


def secure_send(conn, data, key, iv):
    local_data = data
    if len(local_data) == 0:
        local_data = "NOPE"
    import time
    start = time.time()
    cipher = AES.new(key, AES.MODE_CBC, iv)
    encrypted_bytes = cipher.encrypt(pad(local_data))
    print("encrypting time:" + str(time.time() - start))
    start = time.time()
    hash_code = hashlib.md5(encrypted_bytes)
    print("hashing time:" + str(time.time() - start))
    all_data_segments = Segment("hash", hash_code.digest(), False).getBinary() + Segment("result", encrypted_bytes,
                                                         True).getBinary()
    conn.send(all_data_segments)
    # print("DONE!")


def login_request(seg_data):
    segments = pars_segments(seg_data)
    login = ''
    password = ''
    for seg in segments:
        if seg.name == "login":
            login = seg.data.decode('utf-8')
        elif seg.name == "password":
            password = seg.data.decode('utf-8')
    print(Contact().from_login(login, password))
    return Contact().from_login(login, password)


def perform_update_user(seg_data):
    segments = pars_segments(seg_data)
    login = ''
    password = ""
    description = ''
    max_icon = File()
    min_icon = File()
    for seg in segments:
        if seg.name == "login":
            login = seg.data.decode('utf-8')
        if seg.name == "description":
            description = seg.data.decode('utf-8')
        elif seg.name == "password":
            password = seg.data.decode('utf-8')
        elif seg.name == "iconMin":
            min_icon = File().from_segment_data(seg.data)
            print(min_icon.name)
        elif seg.name == "iconMax":
            max_icon = File().from_segment_data(seg.data)
            print(max_icon.name)
    cnt = Contact().from_login(login, password)
    if cnt is None:
        return False
    else:
        cnt.iconMin = min_icon
        cnt.iconMax = max_icon
        cnt.password = password
        cnt.description = description
        cnt.update()
        return True


def perform_news_request(seg_data):
    segments = pars_segments(seg_data)
    news_data = b''
    login = ''
    password = ""
    for seg in segments:
        if seg.name == "news":
            news_data = seg.data
        elif seg.name == "login":
            login = seg.data.decode('utf-8')
        elif seg.name == "password":
            password = seg.data.decode('utf-8')
    print("send request from ", login, password)
    if Contact().from_login(login, password) is not None:
        message = Message().from_segment_data(news_data)
        if message.contact_name != login:
            return False
        message.save_as_news()
    else:
        return False
    return True


def perform_secure_command(conn, data, key, iv):
    segments = pars_segments(data)
    if len(segments) != 1:
        print("why i have ", len(segments), "segments?")
        raise Exception("больше одного сегмента в зашифрованных данных")
    seg = segments[0]
    if seg.name == "newsRequestByNumber":
        secure_send(conn, get_news_by_num(int(seg.data.decode("utf-8"))), key, iv)
    elif seg.name == "loginRequest":
        result = login_request(seg.data)
        if result is None:
            secure_send(conn, b'LOGIN_ERROR', key, iv)
        else:
            secure_send(conn, b'OK', key, iv)
    elif seg.name == "sendNewsRequest":
        if perform_news_request(seg.data):
            print("SEND_SUCCESSFUL")
            secure_send(conn, b'OK', key, iv)
        else:
            print("SEND_ERROR")
            secure_send(conn, b'SEND_ERROR', key, iv)
    elif seg.name == "getContacts":
        secure_send(conn, get_all_contacts_as_segment_data(), key, iv)
    elif seg.name == "updateUserRequest":
        if perform_update_user(seg.data):
            secure_send(conn, b'OK', key, iv)
        else:
            secure_send(b'UPDATE_ERROR', key, iv)


def work_with_input_data(conn, data):
    global new_key
    res = pars_segments(data)
    dict_seg = dict()
    for seg in res:
        dict_seg[seg.name] = seg.data
    key = dict_seg["key"]
    dataToDecrypt = dict_seg["data"]

    pri_bio = BIO.MemoryBuffer(private_key)
    pri_rsa = RSA.load_key_bio(pri_bio)
    AES_key = pri_rsa.private_decrypt(key, RSA.pkcs1_padding)
    iv = dataToDecrypt[:16]
    cipher2 = AES.new(AES_key, AES.MODE_CBC, iv)

    dataToDecrypt = dataToDecrypt[16:]
    result = cipher2.decrypt(dataToDecrypt)
    len_res = parseInt(result[:4])
    if len(result) - 4 < len_res:
        raise Exception("error")
    outputResult = result[4:len_res + 4]
    perform_secure_command(conn, outputResult, AES_key, iv)


sock = socket.socket()
port = 6228
print("port =", port)
sock.bind(('', port))

while True:
    sock.listen(1)
    conn, addr = sock.accept()
    try:
        check = False
        data = b''
        while True:
            if check: break
            data += conn.recv(32)
            if b'SCRTRANSMIT' in data:
                toRead = data[data.index(b'SCRTRANSMIT') + len(b'SCRTRANSMIT'):]
                while len(toRead) < 6:
                    toRead += conn.recv(1)
                if bytes([toRead[0]]) != b'\n':
                    print("WAT!?")
                toRead = toRead[1:]
                length = parseInt(toRead[:4])
                toRead = toRead[4:]
                while len(toRead) < length:
                    toRead += conn.recv(1024)
                work_with_input_data(conn, toRead[:length])
                break
            for cmd in commands:
                if cmd.encode() in data[0:100]:
                    print(len(perform_command(cmd)))
                    print(conn.send(perform_command(cmd)))
                    conn.shutdown(socket.SHUT_RDWR)
                    conn.close()
                    check = True
            if len(data) > 128:
                conn.shutdown(socket.SHUT_RDWR)
                conn.close()
                check = True
                print("overflow")
    except KeyboardInterrupt:
        conn.shutdown(socket.SHUT_RDWR)
        conn.close()
    except BaseException:
        conn.shutdown(socket.SHUT_RDWR)
        conn.close()
