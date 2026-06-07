package io.github.mikhirurg.repl.commands.declarations;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class ClearDeclarations implements REPLCommand {
  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.CLEAR_DECLARATIONS;
  }
}
