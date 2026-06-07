package io.github.mikhirurg.repl.commands.declarations;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class RemoveDeclaration implements REPLCommand {

  private final int number;

  public RemoveDeclaration(int number) {
    this.number = number;
  }

  public int getNumber() {
    return number;
  }

  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.REMOVE_DECLARATION;
  }
}
