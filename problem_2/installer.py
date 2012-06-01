# encoding: utf-8
from platform import system

if __name__ == '__main__':
    system = system()
    if system == 'Linux':
        from dmidecode import dmidecodeXML, DMIXML_DOC
        dmixml = dmidecodeXML()
        dmixml.SetResultType(DMIXML_DOC)
        xmldoc = dmixml.QuerySection('all')
        dmixp = xmldoc.xpathNewContext()
        cpuid = dmixp.xpathEval('/dmidecode/ProcessorInfo/CPUCore/ID')[0].get_content().replace(' ', '')
        mbid = dmixp.xpathEval('/dmidecode/BaseBoardInfo/SerialNumber')[0].get_content()
        fd = open('key', 'w')
        fd.write(mbid + ' ' + cpuid)
        fd.close()
    elif system == 'Windows':
        import wmi
        data = wmi.WMI()
        mbid = data.Win32_BaseBoard()[0].SerialNumber
        cpuid = data.Win32_Processor()[0].ProcessorId
        fd = open('key', 'w')
        fd.write(mbid + ' ' + cpuid)
        fd.close()
    else:
        print 'The OS cannot be determined'
