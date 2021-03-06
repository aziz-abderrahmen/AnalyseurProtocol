package model.protocols;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class TCP extends Protocol {

	private int srcport;
	private int dstport;
	private long seqnum;
	private long acknum;
	private int headerlength;
	private String flags;
	private String reserved;
	private char urg;
	private char ack;
	private char psh;
	private char rst;
	private char syn;
	private char fin;
	private int window;
	private String checksum;
	private int urgpointer;
	private List<String> options;
	private int optionsLength = 0;
	private List<String> tcpData = new ArrayList<>();
	
	//Pour avoir le pseudo ent�te
	
	private List<String> pseudoHeader = new ArrayList<String>();
	private String srcIPHex;
    private String dstIPHex;
	
    public TCP(List<String> data, List<String> pseudoHeader) {
        super(data, 4 * Integer.parseInt(data.get(12).charAt(0)+"",16), 0); 
        srcport = Integer.parseInt((data.get(0) + data.get(1)),16);
        dstport = Integer.parseInt((data.get(2) + data.get(3)),16);
        seqnum = Long.parseLong((data.get(4) + data.get(5) + data.get(6) + data.get(7)),16);
        acknum = Long.parseLong((data.get(8) + data.get(9) + data.get(10) + data.get(11)),16);
        headerlength = Integer.parseInt(data.get(12).charAt(0)+"",16);
        reserved = "0000 00";
        initFlags(data);
        window = Integer.parseInt(data.get(14) + data.get(15),16);
        checksum = data.get(16) + data.get(17);
        urgpointer = Integer.parseInt((data.get(18) + data.get(19)),16);
        int length = 4 * Integer.parseInt(data.get(12).charAt(0)+"",16);
        if(length>20) {
        	options = data.subList(20, length);
        	optionsLength = length - 20;
        }
        tcpData.addAll(data.subList(20 + optionsLength, data.size()));
        initPseudoHeader(data, pseudoHeader);
    }
    
    private void initFlags(List<String> data) {
    	int tmp = Integer.parseInt((data.get(12).charAt(1) + data.get(13)),16);

        // Ajout des chiffres significatifs si besoin
        flags = String.format("%12s", Integer.toBinaryString(tmp)).replace(' ', '0');
        //System.out.println(flags);
        reserved = flags.substring(0,6);
        urg = flags.charAt(6);
        ack = flags.charAt(7);
        psh = flags.charAt(8);
        rst = flags.charAt(9);
        syn = flags.charAt(10);
        fin = flags.charAt(11);
        
        //On remet flags en hexadecimal
        flags = String.format("%3s",new BigInteger(flags, 2).toString(16)).replace(' ', '0');
    }
    
    private void initPseudoHeader(List<String> data, List<String> ps) {
    	pseudoHeader.addAll(ps);
    	pseudoHeader.add("00");	//Zero
    	pseudoHeader.add("06"); //code hexadecimal de TCP
    	String length = String.valueOf(data.size());
    	String l = String.format("%4s",new BigInteger(length).toString(16)).replace(' ', '0');
    	//System.out.println("La taille vaut:" + l.substring(0, 2));
    	pseudoHeader.add(l.substring(0, 2));
    	pseudoHeader.add(l.substring(2, 4)); 
    }
    
    private String toStringOptions(List<String> options) {
    	if(options == null) return "";
    	String res = "\t\tType: 0x" + options.get(0);
    	switch(options.get(0)) {
    		case("00"):
    			res+= " EOL End of Options List\n";
    			break;
    		case("01"):
    			res+= " (NOP) No Operation\n" + toStringOptions(options.subList(1, options.size()));;
    			break;
    		case("02"):
    			String mss = options.get(2) + options.get(3);
    			res+= " (MSS), MSS: 0x" + mss + " (" + Integer.parseInt(mss,16) + ")\n";
    			if(options.size()>4) res+= toStringOptions(options.subList(4, options.size()));
    			break;
    		case("03"):
    			res+= " (WScale) , D�calage : " + options.get(2) + "\n";
    			if(options.size()>3) res+= toStringOptions(options.subList(3, options.size()));
    			break;
    		case("04"):
    			res+= " Sack Permitted\n";
    			if(options.size()>2) res+= toStringOptions(options.subList(2, options.size()));
    			break;
    		case("08"):
    			String tsv = options.get(2) + options.get(3) + options.get(4) + options.get(5);
    			String ter = options.get(6) + options.get(7) + options.get(8) + options.get(9);
    			res+= " (Time Stamp) , TSV : 0x" + tsv + ", TER: 0x" + ter + "\n";
    			if(options.size()>10) res+= toStringOptions(options.subList(10, options.size()));
    			break;
    		case("09"):
    			break;
    		case("0a"):
    			break;
    		case("0e"):
    			break;
   	    			
    	}
    	return res;
    }
    
    
    private String checkChecksum() {
    	/** Addition des hexas de l'entete
         * Si resultat superieur à 16 bits :
         * On additionne les 16 bits "high" avec les 16 bits "low" 
         * res = (somme & 0xFFFF) + (somme >> 16)
         * pas d'erreurs si res = 0xFFFF
         */
    	if (checksum.equals("0000")) return " [validation disabled]";
        List<String> donnee = new ArrayList<>();
        //On y ajoute le pseudo en-t�te et l'entete ainsi que les donn�es
        donnee.addAll(pseudoHeader);
        donnee.addAll(data);
        
        //Si les donn�es sont impaires
        if(donnee.size()%2 == 1) donnee.add("00");
        
        // On cree la liste d'hexas de 2 octets
        List<String> hexs = new ArrayList<>();
        
        
        for(int i = 0; i < donnee.size(); i+=2) {
            hexs.add(donnee.get(i) + donnee.get(i + 1));
        }

        // On fait la somme
        int sum = 0;
        for (String hex : hexs) {
            sum += Integer.parseInt(hex, 16);
        }

        // On somme les bits de poids forts et faibles
        if (sum > 0xFFFF) {
            sum = (sum >> 16) + (sum & 0xFFFF) ;
        }
        //System.out.println("resulat du checksum : " + Integer.toHexString(sum));
        if(sum == 0xFFFF) return " [Correct]";
        else return " [Incorrect]";
    }
    
    
    public String toString() {
    	return "\nTransmission Control Protocol , Src Port: " + srcport  + ", Dst Port:" + dstport + " { \n" 
    				+ "\tSource Port: " + srcport  + "\n"
    				+ "\tDestination Port:" + dstport + "\n"
    				+ "\tSequence Number: " + seqnum + "\n"
    				+ "\t[Next Sequence Number: " + (seqnum+1) + "]\n"
    				+ "\tAcknowledgment Number: " + acknum + "\n"
    				+ "\t" + String.format("%4s", Integer.toBinaryString(headerlength)).replace(' ', '0') + " .... = Header Length : " + (headerlength*4) + " bytes (" + headerlength +")\n" 
    				+ "\tFlags: 0x" + flags + "\n"
    				+ toStringFlags()
    				+ "\tWindow: " + window + "\n"
    				+ "\tChecksum: 0x" + checksum + checkChecksum() + "\n"
    				+ "\tUrgent pointer: " + urgpointer + "\n"
    				+ testOptions(options)
                +'}';
    }
    
    private String testOptions(List<String> options) {
    	if(optionsLength>0) return "\tOptions: (" + optionsLength +" bytes)\n" +  toStringOptions(options);
    	return "";
    }
    
    private String toStringPayload() {
    	if (getPayload()!=null) return "\tTCP payload (" + getPayload().size() + " bytes)\n";
    	return "";
    }
    
    private String toStringFlags() {
    	String not = "Not ";
    	String res = "\t\t" + reserved + ".. .... = Reserved : ";
    	if(reserved.equals("0")) res += not;
    	res+= "set\n";
    	
    	res+= "\t\t" + ".... .." + urg + ". .... = Urgent : ";
    	if(urg == '0') res += not;
    	res+= "set\n";
    	
    	res+= "\t\t"+ ".... ..." + ack + " .... = Acknowledgment : ";
    	if(ack == '0') res += not;
    	res+= "set\n";
    	

    	res+= "\t\t"+ ".... .... " + psh + "... = Push : ";
    	if(psh == '0') res += not;
    	res+= "set\n";
    	
    	res+= "\t\t"+ ".... .... ." + rst + ".. = Reset : ";
    	if(rst == '0') res += not;
    	res+= "set\n";
    	
    	res+= "\t\t"+ ".... .... .." + syn + ". = Syn : ";
    	if(syn == '0') res += not;
    	res+= "set\n";
    	
    	res+= "\t\t"+ ".... .... ..." + fin + " = Fin : ";
    	if(fin == '0') res += not;
    	res+= "set\n";
    	
    	return res;
    }
    
}
