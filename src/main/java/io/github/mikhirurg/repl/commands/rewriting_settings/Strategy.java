package io.github.mikhirurg.repl.commands.rewriting_settings;

import cora.config.Settings;
import io.github.mikhirurg.repl.commands.REPLCommand;

public class Strategy implements REPLCommand {
  private final Settings.Strategy strategy;

  public Strategy(Settings.Strategy strategy) {
    this.strategy = strategy;
  }

  public Settings.Strategy getStrategy() {
    return strategy;
  }

  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.STRATEGY;
  }
}
