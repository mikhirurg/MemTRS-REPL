package io.github.mikhirurg.repl.commands.imports;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class ListImports implements REPLCommand {
  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.LIST_IMPORTS;
  }
}
