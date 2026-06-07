package io.github.mikhirurg.repl.commands.imports;

import io.github.mikhirurg.repl.commands.REPLCommand;

public class Include implements REPLCommand {
  private final String path;

  public Include(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }

  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.INCLUDE;
  }
}
