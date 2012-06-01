#!/usr/bin/python
#encoding: utf-8
import argparse, sys, os

parser = argparse.ArgumentParser(description='Прячет любые файлы внутри MP3')
parser.add_argument('file', default=sys.stdin)
parser.add_argument('mp3', default=sys.stdout)
group = parser.add_mutually_exclusive_group()
group.add_argument('--hide', action='store_true')
group.add_argument('--extract', action='store_true')

bitrate_tbl = {'0001': {'1101': 32, '1001': 8},
           '0010': {'1101': 40, '1001': 16},
           '0011': {'1101': 48, '1001': 24},
           '0100': {'1101': 56, '1001': 32},
           '0101': {'1101': 64, '1001': 40},
           '0110': {'1101': 80, '1001': 48},
           '0111': {'1101': 96, '1001': 56},
           '1000': {'1101': 112, '1001': 64},
           '1001': {'1101': 128, '1001': 80},
           '1010': {'1101': 160, '1001': 96},
           '1011': {'1101': 192, '1001': 112},
           '1100': {'1101': 224, '1001': 128},
           '1101': {'1101': 256, '1001': 144},
           '1110': {'1101': 320, '1001': 160}
          }

sample_tbl = {'0011': 44100,
              '0111': 48000,
              '1011': 32000,
              '0010': 22050,
              '0110': 24000,
              '1010': 16000
             }

def size2bin(size):
    a = []
    s = bin(size)[2:].rjust(32, '0')
    print s
    a.append(int(s[0:8], 2))
    a.append(int(s[8:16], 2))
    a.append(int(s[16:24], 2))
    a.append(int(s[24:32], 2))
    return a

def toBits(f):
    return bin(ord(f.read(1)))[2:].rjust(8, '0')

def frameNum(f):
    frames = 0
    try:
        while True:
            header = ''
            for i in xrange(4):
                header += toBits(f)
            sync = header[0:11]
            if sync != '11111111111':
                raise Exception('Not syncing')
            version = header[11:13]
            layer = header[13:15]
            protection = header[15:16]
            rate_idx = header[16:20]
            sample = header[20:22]
            padding = header[22:23]
            private = header[23:24]
            mode = header[24:26]
            mode_ext = header[26:28]
            copyright = header[28:29]
            original = header[29:30]
            emphasis = header[30:32]
            bitrate = bitrate_tbl[rate_idx][version + layer]
            samplerate = sample_tbl[sample + version]
            length = 144000 * bitrate / samplerate + int(padding)
            f.seek(-4, 1)
            f.read(length)
            frames = frames + 1
    except:
        return frames
    return frames

def id3tag(f):
    magic = f.read(3)
    if magic == 'ID3':
        f.seek(6)
        size = ''
        for i in xrange(4):
            size = size + bin(ord(f.read(1)))[2:].rjust(7, '0')
        size = int(size, 2) + 10
        f.seek(0)
        buf = f.read(size)
        return buf
    else:
        f.seek(0)
        return False

def hide(args):
    out = open('stego-'+args.mp3, 'wb')
    inf = open(args.file, 'rb')
    inm = open(args.mp3, 'rb')
    buf = id3tag(inm)
    if buf:
        out.write(buf)
    frames = frameNum(inm)
    buf = inf.read()
    in_size = len(buf)
    if frames - 10 >= in_size:
        block = 1
    else:
        if in_size % (frames - 10)!= 0:
            block = in_size / (frames - 10) + 1
        else:
            block = in_size / (frames - 10)
    size = size2bin(block)
    print size
    for i in xrange(4):
        out.write('%c' % size[i])
    size = size2bin(in_size)
    print size
    for i in xrange(4):
        out.write('%c' % size[i])
    print in_size, block
    inf.seek(0)
    inm.seek(0)
    id3tag(inm)
    for j in xrange(frames):
        header = ''
        for i in xrange(4):
            header += toBits(inm)
        sync = header[0:11]
        if sync != '11111111111':
            print 'Not syncing'
            raise Exception('Not syncing')
        version = header[11:13]
        layer = header[13:15]
        protection = header[15:16]
        rate_idx = header[16:20]
        sample = header[20:22]
        padding = header[22:23]
        private = header[23:24]
        mode = header[24:26]
        mode_ext = header[26:28]
        copyright = header[28:29]
        original = header[29:30]
        emphasis = header[30:32]
        bitrate = bitrate_tbl[rate_idx][version + layer]
        samplerate = sample_tbl[sample + version]
        length = 144000 * bitrate / samplerate + int(padding)
        inm.seek(-4, 1)
        out.write(inm.read(length))
        if j > 10:
            out.write(inf.read(block))
    out.write(inm.read())
    out.close()
    inf.close()
    inm.close()

def extract(args):
    out = open('extracted-'+args.file, 'wb')
    inp = open(args.mp3, 'rb')
    print args.mp3
    id3tag(inp)
    print inp.tell()
    size = ''
    for i in xrange(4):
        size = size + bin(ord(inp.read(1)))[2:].rjust(8, '0')
        print size
    print size
    block = int(size, 2)
    size = ''
    for i in xrange(4):
        size = size + bin(ord(inp.read(1)))[2:].rjust(8, '0')
        print size
    filesize = int(size, 2)
    print size
    print block, filesize
    total = 0
    j = 0
    while total < filesize:
        header = ''
        for i in xrange(4):
            header += toBits(inp)
        sync = header[0:11]
        if sync != '11111111111':
            print 'Not syncing'
            raise Exception('Not syncing')
        version = header[11:13]
        layer = header[13:15]
        protection = header[15:16]
        rate_idx = header[16:20]
        sample = header[20:22]
        padding = header[22:23]
        private = header[23:24]
        mode = header[24:26]
        mode_ext = header[26:28]
        copyright = header[28:29]
        original = header[29:30]
        emphasis = header[30:32]
        bitrate = bitrate_tbl[rate_idx][version + layer]
        samplerate = sample_tbl[sample + version]
        length = 144000 * bitrate / samplerate + int(padding)
        inp.seek(-4, 1)
        inp.read(length)
        if j > 10:
            tmp = inp.read(block)
            out.write(tmp)
            total = total + block
            if (filesize - total) < block:
                block = filesize - total
        j = j + 1
    out.close()
    inp.close()    

def main():
    args = parser.parse_args()
    if args.hide:
        hide(args)
    elif args.extract:
        extract(args)
main()
