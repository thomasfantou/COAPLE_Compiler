package co.uk.brookes.codegeneration.builder;

import java.util.ArrayList;

/**
 * Author: Fantou Thomas
 * Date: 7/2/13
 */
public class Constant {
    ArrayList<Instruction> labels = new ArrayList<Instruction>();
    ArrayList<Instruction> states = new ArrayList<Instruction>();
    ArrayList<Instruction> actions = new ArrayList<Instruction>();
    ArrayList<Instruction> envs = new ArrayList<Instruction>();
}
