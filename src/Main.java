
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
//test file path: /Users/raha/Desktop/test.txt

public class Main {
    public static void main(String[] args) {

        Dictionary<String, String> opDict = new Hashtable<>();
        opDict.put("add", "000000");
        opDict.put("sub", "001010");
        opDict.put("and", "001000");
        opDict.put("or", "000010");
        opDict.put("xor", "001100");
        String[] registers = {"eax", "ax", "al", "ecx", "cx", "cl", "edx", "dx", "dl", "ebx", "bx", "bl", "esp", "sp", "ah", "ebp", "bp", "ch", "esi", "si", "dh", "edi", "di", "bh"};
        Dictionary<String, String> regDict = new Hashtable<>();
        Dictionary<String, Integer> regSize = new Hashtable<>();
        int i = 0;
        double size = 2;
        int counter = 0;
        for (String register : registers) {
            regDict.put(register, beautifulBinaryRegisterOP(Integer.toBinaryString(i)));
            regSize.put(register, (int) Math.pow(2, size));
            counter++;
            if (counter % 3 == 0)
                i++;
            size = 2 - counter % 3;
        }
        System.out.println(regDict.get("al"));
        System.out.println(regDict.get("bl"));

        System.out.println(regSize.get("al"));
        System.out.println(regSize.get("bl"));
        //*********************************************************************************
        Dictionary<String, String> labelDict = new Hashtable<>();
        Dictionary<Integer, String[]> jmpDict = new Hashtable<>();

        String answer;
        String line ="";

        Scanner scanner = new Scanner(System.in);
        System.out.println("Press 1 to enter instructions manually and any other digit to to read from file");
        int option = scanner.nextInt();
        scanner.nextLine();
        if (option == 1) {
            while (true) {
                System.out.println("Enter your instruction Enter 'exit' to exit the program");
                line = scanner.nextLine().toLowerCase();
                if(line.equals("exit"))
                    break;
                answer = "";
                String[] instruction;
                instruction = line.split("[,  ]+");
                if (instruction.length == 3) {
                    answer += twoOperandAssembler(regSize, opDict, regDict, instruction);
                } else if (instruction.length == 2) {
                    answer += oneOperandAssembler(regSize, opDict, regDict, instruction);
                }
                if(answer.equals(""))
                    System.out.println("invalid input");
                System.out.println(answer);
            }

        } else {
            System.out.println("Please enter your file path: ");
            String filePath = scanner.nextLine();
            System.out.println("Please enter the path where you want your answer file to be: ");
            String outputFilePath = scanner.nextLine();
            try {
                File outputFile = new File(outputFilePath);
                if (outputFile.createNewFile()) {
                    System.out.println("File created: " + outputFile.getName());
                } else {
                    System.out.println("File already exists.If you want to proceed and modify the files content enter 1");
                    int ans = scanner.nextInt();
                    if (ans != 1)
                        return;
                }
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }

            String finalAnswer;
            String[] jmpParameters = new String[2];
            String address = "0000000000000000";
            int fileline = 0;

            //This arraylist will store all the obfuscated instructions
            ArrayList<String> instructions = new ArrayList<>();

            //opening the file
            Path path = Paths.get(filePath);
            Path outputPath = Paths.get(outputFilePath);
            Charset charset = Charset.forName("US-ASCII");

            try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
                while ((line = reader.readLine()) != null) {
                    line = line.toLowerCase();
                    answer = "";
                    String[] instruction;
                    instruction = line.split("[,  ]+");
                    if (instruction.length == 3) {
                        answer += twoOperandAssembler(regSize, opDict, regDict, instruction);
                    } else if (instruction.length == 2) {
                        if (instruction[0].equals("jmp")) {
                            jmpParameters[0] = address;//offset of the jump instruction
                            jmpParameters[1] = instruction[1];//the label jump wants to go to
                            jmpDict.put(fileline, jmpParameters);
                        } else
                            answer += oneOperandAssembler(regSize, opDict, regDict, instruction);
                    } else {
                        //it must be a label or empty line
                        if (instruction[0].charAt(instruction[0].length() - 1) == ':') {
                            //System.out.println(address);
                            labelDict.put(instruction[0].replace(":", ""), address);
                        }
                        answer += " ";
                    }
                    if (answer.split(":")[0].equals("Error")) {
                        System.out.println(answer);
                        return;
                    }
                    finalAnswer = address + " " + answer;
                    address = getInstructionAddress(address, answer);
                    //append the answer to instructions array
                    instructions.add(finalAnswer);
                    //this variable shows what line we are currently at
                    fileline++;

                }

            } catch (IOException x) {
                System.err.format("IOException: %s%n", x);
            }

            String jmpOffset;
            String labelOffset;
            int index;

            Enumeration<Integer> keys = jmpDict.keys();
            while (keys.hasMoreElements()) {
                index = keys.nextElement();
                labelOffset = labelDict.get(jmpDict.get(index)[1]);
                jmpOffset = jmpDict.get(index)[0];
                answer = jmpOffset + " " + jmpOpcode(labelOffset, jmpOffset);
                instructions.set(index, answer);
            }
            //*********************************************************************************
            //writing the answers into file
            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, charset)) {
                for (String ans : instructions) {
                    writer.write(ans, 0, ans.length());
                    writer.newLine();
                }
            } catch (IOException x) {
                System.err.format("IOException: %s%n", x);
            }
        }
    }
    //*********************************************************************************
    //methods
    public static String findS (Dictionary<String,Integer> regSize,String register){
        //checks the size of the register and returns the s bit for the opcode
        if (regSize.get(register)==1)
            return "0";
        return "1";
    }
    public static String  changeToSpacedLittleEndian(String number){
        //adding zero to the beginning if the answer doesn't have it
        if(number.length()<8){
            for(int i=0;i<16-number.length()-1;i++)
            number = "0"+ number;
        }
        String answer = new String();
        int counter = 0;
        for(int i = number.length()-1;i>0;i--){
            if(counter==1 && i!=0){
                answer+=" ";
                counter = 0;
            }
            answer += number.charAt(i-1);
            answer+=number.charAt(i);
            counter++;
            i--;
        }
        return answer;
    }

    public static int whichIsIndirectAddressing(String[] instruction){
        if(instruction.length==3){
            if(instruction[1].charAt(0)=='[')
                return 1;
            else if(instruction[2].charAt(0)=='[')
                return 2;
        }
        else{
            if(instruction[1].charAt(0)=='[')
                return 1;
        }
        return 0;
    }

    public static String twoOperandAssembler(Dictionary<String,Integer> regSize,Dictionary<String, String> opDict,Dictionary<String, String> regDict,String[] instruction){
        String answer = new String();
        String tempAnswer = new String();
        int registerSize=0;
        int indirectAddressingRegister = whichIsIndirectAddressing(instruction);
        //checking if the instruction has two registers or one register and one indirect address
        if(indirectAddressingRegister==0) {
            if (opDict.get(instruction[0]) != null) {
                if (regDict.get(instruction[1]) == null || regDict.get(instruction[2]) == null) {
                    return "Error: The operation isn't between two registers";
                }
                if ((registerSize = regSize.get(instruction[1])) != regSize.get(instruction[2])) {
                    return "Error: The operation is between two registers of different size";
                }
                //adding prefix if the register is 16 bit
                if (registerSize == 2)
                    answer += "66 ";
                tempAnswer += opDict.get(instruction[0]);
                //finding d bit of op code
                //because we are supporting the shell-storm format I put the d bit 0
                tempAnswer += "0";
                //finding s bit of opcode
                tempAnswer += findS(regSize, instruction[1]);
                answer += beautifulHex(Integer.toHexString(Integer.valueOf(Integer.parseInt(tempAnswer, 2))));
                answer += " " + twoOperandInstructionMODRM(regDict, instruction);
            }
            //Indirect addressing
        } else {
            //checking if the given string is actually a register or a number or indirect address
            if(indirectAddressingRegister==1){
                if(regDict.get(instruction[2])==null || regDict.get(getIndirectAddressingRegister(instruction[1]))==null)
                    return "Error: the given instruction operands are not supported registers";
            }
            else{
                if(regDict.get(instruction[1])==null || regDict.get(getIndirectAddressingRegister(instruction[2]))==null)
                    return "Error: the given instruction operands are not supported registers";
            }
            //****************************************************************************************************
            //this part is for two operand instructions with one indirect addressing op code
            tempAnswer += opDict.get(instruction[0]);
            if(indirectAddressingRegister==1){
                tempAnswer+="0";
                registerSize = regSize.get(instruction[2]);
            }
            if(indirectAddressingRegister==2){
                tempAnswer+="1";
                registerSize = regSize.get(instruction[1]);
            }
            //determining d bit of the opcode 8bits based on the size of the register
            if(registerSize==1)
                tempAnswer+="0";
            else if (registerSize==2){
                answer += "66 ";
                tempAnswer+="1";
            }
            else
                tempAnswer+="1";
            answer += beautifulHex(Integer.toHexString(Integer.valueOf(Integer.parseInt(tempAnswer, 2))));
            //now finding the modr/m byte
            answer += " " + twoOperandInstructionMODRM(regDict, instruction);
        }
        return answer;
    }
    public static String oneOperandAssembler(Dictionary<String,Integer> regSize,Dictionary<String, String> opDict,Dictionary<String, String> regDict,String[] instruction){
        String answer = new String();
        int registerSize=0;
        int indirectAddressingRegister = whichIsIndirectAddressing(instruction);
        //checking if the instruction has two operands
        if(indirectAddressingRegister==0) {
            if(regSize.get(instruction[1])!=null) //this is only for the immediate case of push
                registerSize = regSize.get(instruction[1]);
            if(instruction[0].equals("inc")){
                if(registerSize==1)
                    answer+= "fe ";
                else if(registerSize==2)
                    answer+= "66 ";
                //modr/m bit for 16/32 increase is the register plus 64 in decimal
                if(registerSize==1){
                    answer += beautifulHex(Integer.toHexString(192+Integer.parseInt(regDict.get(instruction[1]),2)));
                }
                else {
                answer += beautifulHex(Integer.toHexString(64+Integer.parseInt(regDict.get(instruction[1]),2)));
                }
            }
            else if(instruction[0].equals("dec")){
                if(registerSize==1)
                    answer+= "fe ";
                else if(registerSize==2)
                    answer+= "66 ";
                //modr/m bit for increase is the register plus 64 in decimal
                if(registerSize==1){
                    answer += beautifulHex(Integer.toHexString(200+Integer.parseInt(regDict.get(instruction[1]),2)));
                }
                else{
                answer += beautifulHex(Integer.toHexString(72+Integer.parseInt(regDict.get(instruction[1]),2)));
                }
        }
        else if(instruction[0].equals("push")){
            //finding the op code for immediate
                //checking if the operand is an immediate
                if(Integer.valueOf(instruction[1].charAt(0))<='9' && Integer.valueOf(instruction[1].charAt(0))>='0'){
                    answer += "68 ";
                    answer += changeToSpacedLittleEndian(Integer.toHexString(Integer.valueOf(instruction[1])));
                }
            else{
            if(registerSize==1)
                return "Error: can't push 8bit register";
            else if(registerSize==2)
                answer+="66 ";

            answer += beautifulHex(Integer.toHexString(80 + Integer.parseInt(regDict.get(instruction[1]),2)));
            }

        } else if (instruction[0].equals("pop")) {
                if(registerSize==1)
                    return "Error: can't pop 8bit register";
                else if(registerSize==2)
                    answer+="66";
                answer += beautifulHex(Integer.toHexString(88 + Integer.parseInt(regDict.get(instruction[1]),2)));
            }
    }
        else{
            if(instruction[0].equals("push")){
                if(regSize.get(getIndirectAddressingRegister(instruction[1]))!=4)
                    return "Error: Can not perform push instruction on 8bit or 16bit registers";
                answer +="ff ";
                answer += beautifulHex(Integer.toHexString(48 + Integer.parseInt(regDict.get(getIndirectAddressingRegister(instruction[1])),2)));
            }
            else
                return "Error: Indirect addressing can not be used for this instruction";
        }
        return answer;
    }

    public static String twoOperandInstructionMODRM(Dictionary<String, String> regDict,String[] instruction){
        //I am assuming that it already is checked if both are registers ( so I only have 11 as MOD )
        String answer = new String();
        int indirectAddressingRegister = whichIsIndirectAddressing(instruction);
        if(indirectAddressingRegister==0){
            answer += "11";
            //the REG bit is the destination register
            answer += regDict.get(instruction[2]);
            //the R/M bit is the source register
            answer += regDict.get(instruction[1]);
           }
        else{
            //the mod is always 00 for indirect addressing without displacement
            answer += "00";
            if(indirectAddressingRegister==1){
                answer += regDict.get(instruction[2]);
                answer += regDict.get(getIndirectAddressingRegister(instruction[1]));
            }
            else {
                answer += regDict.get(instruction[1]);
                answer += regDict.get(getIndirectAddressingRegister(instruction[2]));
            }
        }
        //turning the string hexadecimal
        return beautifulHex(Integer.toHexString(Integer.valueOf(Integer.parseInt(answer, 2))));
    }

    public static String getIndirectAddressingRegister(String IndirectAddressing){
        String answer = new String();
        answer = IndirectAddressing.replace("[","");
        answer = answer.replace("]","");
        return answer;
    }
    //this method will add 0 to the beginning of hex numbers if they are ommited
    public static String beautifulHex(String string){
        int len = string.length();
        if(len>2){
            String temp = new String();
            temp += string.charAt(len-2);
            temp += string.charAt(len-1);
            return temp;
        }
        if(string.length()==1)
            string = "0" + string;
        return string;
    }
    public static String getInstructionAddress(String previousInstructionAddress,String answer){
        String temp ;
        if(answer.equalsIgnoreCase(" "))
            return previousInstructionAddress;
        int size  =  answer.split(" ").length;
        temp = Integer.toHexString(size + Integer.valueOf(Integer.parseInt(previousInstructionAddress, 16)));
        int len = temp.length();
        for(int i = 0;i<16-len;i++)
            temp = "0"+temp;
        return temp;
    }
    //method to find the answer for jmp instruction using offsets
    public static String jmpOpcode(String labelOffset,String jmpOffset){
        String answer = "eb ";
        int jmp = Integer.valueOf(Integer.parseInt(labelOffset, 16)) - Integer.valueOf(Integer.parseInt(jmpOffset, 16));
        if(jmp >127 || jmp<-127)
            return "Error: the instruction is not a short jump";
        if(jmp<0) //it is a backward jump
            jmp +=2;
        else  //it is a forward jump
            jmp -=2;
        System.out.println(jmp);
        answer += beautifulHex(Integer.toHexString(jmp));
        return answer;
    }
    public static String beautifulBinaryRegisterOP(String instructionCode){
        while (instructionCode.length()<3)
            instructionCode = "0"+instructionCode;
        return instructionCode;
    }
}
