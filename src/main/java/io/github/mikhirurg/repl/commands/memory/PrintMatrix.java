package io.github.mikhirurg.repl.commands.memory;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class PrintMatrix implements REPLCommand {

  private final int addr;

  public PrintMatrix(int addr) {
    this.addr = addr;
  }

  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.PRINT_MATRIX;
  }

  public int getAddr() {
    return addr;
  }
}
