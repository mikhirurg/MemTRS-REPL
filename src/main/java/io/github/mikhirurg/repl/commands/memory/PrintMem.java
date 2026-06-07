package io.github.mikhirurg.repl.commands.memory;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class PrintMem implements REPLCommand {
  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.PRINT_MEM;
  }
}
