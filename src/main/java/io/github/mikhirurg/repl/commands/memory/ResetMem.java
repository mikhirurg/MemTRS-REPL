package io.github.mikhirurg.repl.commands.memory;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class ResetMem implements REPLCommand {
  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.RESET_MEM;
  }
}
