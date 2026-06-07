package io.github.mikhirurg.repl.commands.general;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class ListTRS implements REPLCommand {
  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.TRS;
  }
}
