package io.github.mikhirurg.repl.commands.general;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class Help implements REPLCommand {

  private final String command;

  public Help() {
    this(null);
  }

  public Help(String command) {
    this.command = command;
  }

  public String getCommand() {
    return command;
  }

  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.HELP;
  }
}
