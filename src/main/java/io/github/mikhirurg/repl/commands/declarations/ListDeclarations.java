package io.github.mikhirurg.repl.commands.declarations;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class ListDeclarations implements REPLCommand {
  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.LIST_DECLARATIONS;
  }
}
