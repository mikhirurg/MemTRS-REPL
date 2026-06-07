package io.github.mikhirurg.repl.commands.memory;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class PrintArray implements REPLCommand {

  private final int addr;

  public PrintArray(int addr) {
    this.addr = addr;
  }

  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.PRINT_ARRAY;
  }

  public int getAddr() {
    return addr;
  }
}
