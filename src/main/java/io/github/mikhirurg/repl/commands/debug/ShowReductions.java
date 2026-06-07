package io.github.mikhirurg.repl.commands.debug;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class ShowReductions implements REPLCommand {
  private final boolean isEnabled;

  public ShowReductions(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.SHOW_REDUCTIONS;
  }
}
