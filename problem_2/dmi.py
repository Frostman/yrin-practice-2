# encoding: utf-8
from platform import system
from sys import exit

if __name__ == '__main__':
    try:
        fd = open('key', 'r')
    except:
        print 'Key is not found'
        exit(1)
    else:
        id = fd.read().split(' ')
        fd.close()
        
    system = system()
    
    if system == 'Linux':
        from dmidecode import dmidecodeXML, DMIXML_DOC
        dmixml = dmidecodeXML()
        dmixml.SetResultType(DMIXML_DOC)
        xmldoc = dmixml.QuerySection('all')
        dmixp = xmldoc.xpathNewContext()
        
        chassis = dmixp.xpathEval('/dmidecode/ChassisInfo/ChassisType')[0].get_content()
        print 'Chassis: %s' % chassis
        
        cpuid = dmixp.xpathEval('/dmidecode/ProcessorInfo/CPUCore/ID')[0].get_content().replace(' ', '')
        mbid = dmixp.xpathEval('/dmidecode/BaseBoardInfo/SerialNumber')[0].get_content()
        
        if mbid != id[0] and cpuid != id[1]:
            print 'The program has been installed on other computer'
        else:
            print 'OK!'
        
    elif system == 'Windows':
        import wmi
        data = wmi.WMI()
        print data.keys
        mbid = data.Win32_BaseBoard()[0].SerialNumber
        cpuid = data.Win32_Processor()[0].ProcessorId
        if mbid != id[0] and cpuid != id[1]:
            print 'The program has been installed on other computer'
        else:
            print 'OK!'   
    else:
        print 'OS cannot be determined'
    
