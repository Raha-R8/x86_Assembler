import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MemoryPrint {
    //this class contains methods for phase2 of the project
    //as you can tell I did not determine the address here and I think I should work on it later when I'm writing to file
    public static ArrayList<String> writeDataSegment(ArrayList<String> dataSegmentInput) {
        Dictionary<String, Integer> memorySizeDict = new Hashtable<>();
        memorySizeDict.put("byte", 1);
        memorySizeDict.put("sbyte", 1);
        memorySizeDict.put("word", 2);
        memorySizeDict.put("sword", 2);
        memorySizeDict.put("dword", 4);
        memorySizeDict.put("sdword", 4);
        ArrayList<String> dataSegment = new ArrayList<>();
        for (String line : dataSegmentInput) {
            String[] lineData = line.split(" ");
            int i = memorySizeDict.get(lineData[1].toLowerCase());
            for (int m = 0; m < i; m++) {
                dataSegment.add(lineData[0]);
            }
        }
        //adding labels to data segment
        for (String label:Main.jumpLabels) {
            dataSegment.add(label);
        }
        return dataSegment;
    }


    //the input for this segment should be push and pop lines that are read from the first file


    //given number is in hexadecimal and at most 32 bit
    public static ArrayList<String> changeToLittleEndian(String number) {
        //removing any zeros behind the number
        while (number.charAt(0) == '0' && number.length() > 1) {
            number = number.substring(1);
        }
        int len = number.length();
        //adding zero to the beginning if the answer doesn't have it
        if (len <= 2) {
            number = "0" + number;
        } else if (len <= 4) {
            for (int i = 0; i < 4 - len; i++)
                number = "0" + number;
        } else if (len <= 8) {
            for (int i = 0; i < 8 - len; i++)
                number = "0" + number;
        } else {
            //this is in case the number is bigger than 32 bits
            return null;
        }
        ArrayList<String> finalAnswer = new ArrayList<>();
        for (int i = number.length() - 1; i > 0; i--) {
            String answer = "";
            answer += number.charAt(i - 1);
            answer += number.charAt(i);
            i--;
            finalAnswer.add(answer);
        }
        return finalAnswer;
    }

    public static ArrayList<String> writeStackSegment( Dictionary<String, Integer> regSize, ArrayList<String> stackSegmentInput) {
        ArrayList<String> stackSegment = new ArrayList<>();
        for (String line : stackSegmentInput) {
            int size = 0;
            String[] lineData = line.split(" ");
            switch (lineData[0]) {
                case "push":
                    //what is being pushed is a number
                    if (Integer.valueOf(lineData[1].charAt(0)) <= '9' && Integer.valueOf(lineData[1].charAt(0)) >= '0') {
                        ArrayList<String> number = changeToLittleEndian(lineData[1]);
                        for (String string : number) {
                            stackSegment.add(string);
                        }
                        for(int i = 0;i<(4-number.size());i++)
                            stackSegment.add("MM");
                    } else if ((size = regSize.get(lineData[1].toLowerCase())) != 0) {
                        for (int i = 0; i < size; i++) {
                            stackSegment.add(lineData[1].toLowerCase());
                        }
                        for (int i = 0;i<4-size;i++){
                            stackSegment.add("MM");
                        }
                    }
                    break;
                case "pop":
                    //removing 4 bytes from the stack each time
                    for (int i = 0; i < 4; i++) {
                        stackSegment.remove(stackSegment.size() - 1);
                    }
                    break;
            }
        }
        return stackSegment;
    }

    public static ArrayList<String> writeCodeSegment(ArrayList<String> codeSegmentInput, Dictionary<String, Integer> regSize, Dictionary<String, String> opDict, Dictionary<String, String> regDict) {

        //*********************************************************************************
        Dictionary<String, String> labelDict = new Hashtable<>();
        Dictionary<Integer, String[]> jmpDict = new Hashtable<>();

        String answer;
        int offset = 0;
        String address = "0000000000000000";
        //This arraylist will store all the obfuscated instructions
        ArrayList<String> instructions = new ArrayList<>();

        for (String line : codeSegmentInput) {
            line = line.toLowerCase();
            answer = "";
            String[] instruction;
            instruction = line.split("[,  ]+");
            if (instruction.length == 3) {
                answer += Main.twoOperandAssembler(regSize, opDict, regDict, instruction);
            } else if (instruction.length == 2) {
                if (instruction[0].equals("jmp")) {
                    String[] jmpParameters = new String[2];
                    jmpParameters[0] = address;//offset of the jump instruction
                    jmpParameters[1] = instruction[1];//the label jump wants to go to

                    jmpDict.put(instructions.size(), jmpParameters);
                    //this is put here just to fix the issue of address increment of jump
                    instructions.add("");//place for instruction op code
                    instructions.add(" ");//place for instruction label offset
                    address = Main.getInstructionAddress(address, answer, instruction[0]);
                    continue;

                } else answer += Main.oneOperandAssembler(regSize, opDict, regDict, instruction);
            } else {
                //it must be a label or empty line
                if (instruction[0].charAt(instruction[0].length() - 1) == ':') {
                    //System.out.println(address);
                    labelDict.put(instruction[0].replace(":", ""), address);
                }
                answer += " ";
            }
            String[] splitAnswer = answer.split(" ");
//            System.out.println(answer);
//            System.out.println("***********");
            //checking for answer being empty string because of label

            for (int i = splitAnswer.length - 1; i >= 0; i--) {
                instructions.add(splitAnswer[i]);;
            }
            //adding empty byte because this segment is even aligned
            if(instructions.size()%2!=0)
                instructions.add("MM");

            address = Main.getInstructionAddress(address, answer, instruction[0]);
            //this variable shows what line we are currently at
        }

        String jmpOffset;
        String labelOffset;
        int index;

        Enumeration<Integer> keys = jmpDict.keys();
        while (keys.hasMoreElements()) {
            index = keys.nextElement();
            labelOffset = labelDict.get(jmpDict.get(index)[1]);
            jmpOffset = jmpDict.get(index)[0];
            answer = jmpOffset + " " + Main.jmpOpcode(labelOffset, jmpOffset);

            String[] splitAnswer = answer.split(" ");
            Main.jumpLabels.add(jmpDict.get(index)[1]+" "+"#"+splitAnswer[2]);
            for (int i = splitAnswer.length - 1; i >= 0; i--) {
                instructions.set(index, splitAnswer[2]);
                instructions.set(index+1, splitAnswer[1]);
            }
        }

        return instructions;
    }

}
