package io.github.mikhirurg.repl.commands.rules;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class ListRules implements REPLCommand {
  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.LIST_RULES;
  }
}
