from abc import ABC, abstractmethod
from enum import Enum

class OpCode(Enum):
  LI = "LI"
  LA = "LA"
  ADD = "ADD"
  SUB = "SUB"
  DIV = "DIV"
  MUL = "MUL"
  NEG = "NEG"
  MV = "MV"
  LW = "LW"
  SW = "SW"
  PUTS = "PUTS"
  PUTI = "PUTI"
  GETI = "GETI"
  HALT = "HALT"
  ADDI = "ADDI"

  # BRANCH INSTRUCTIONS
  BEQ = "BEQ"
  BGE = "BGE"
  BGT = "BGT"
  BLE = "BLE"
  BLT = "BLT"
  BNE = "BNE"
  J = "J"

  # FLOAT INSTRUCTIONS
  FADDS = "FADD.S"
  FSUBS = "FSUB.S"
  FDIVS = "FDIV.S"
  FMULS = "FMUL.S"
  FMVS = "FMV.S"
  FNEGS = "FNEG.S"
  FLW = "FLW"
  FSW = "FSW"
  GETF = "GETF"
  PUTF = "PUTF"
  FIMMS = "FIMM.S"
  FLT = "FLT.S"
  FLE = "FLE.S"
  FEQ = "FEQ.S"

  # FUNCTION CALL AND RETURN
  JR = "JR"
  RET = "RET"

  def __init__(self, name: str):
    self.opCodeName = name

  def __str__(self):
    return self.opCodeName


class Instruction(ABC):
  def __init__(self):
    self.src1  = None
    self.src2  = None
    self.dest  = None
    self.label = None
    self.oc    = None

  def getDest(self):
    return self.dest

  class Operand(Enum):
    SRC1 = "SRC1"
    SRC2 = "SRC2"
    DEST = "DEST"

  def getOC(self) -> OpCode:
    return self.oc

  def getOperand(self, o: Operand) -> str:
    if o == self.Operand.SRC1:
      return self.src1
    elif o == self.Operand.SRC2:
      return self.src2
    elif o == self.Operand.DEST:
      return self.dest
    else:
      raise Exception("Shouldn't get here")

  def getLabel(self) -> str:
    return self.label

  def is3AC(self, o: Operand = None) -> bool:
    if o is not None:
      if o == self.Operand.SRC1:
        return Instruction.is3AC(self.src1)
      elif o == self.Operand.SRC2:
        return Instruction.is3AC(self.src2)
      elif o == self.Operand.DEST:
        return Instruction.is3AC(self.dest)
      else:
        raise Exception("Shouldn't get here")
    else:
      return self.is3AC(self.Operand.SRC1) or self.is3AC(self.Operand.SRC2) or self.is3AC(self.Operand.DEST)
  
  @staticmethod
  def is3AC(s: str) -> bool:
    return (s is not None) and (s[0] == '$')
