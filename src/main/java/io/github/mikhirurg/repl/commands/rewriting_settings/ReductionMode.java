package io.github.mikhirurg.repl.commands.rewriting_settings;

import cora.config.Settings;
import io.github.mikhirurg.repl.commands.REPLCommand;

public class ReductionMode implements REPLCommand {
  private final Settings.ReductionMode reductionMode;

  public ReductionMode(Settings.ReductionMode mode) {
    this.reductionMode = mode;
  }

  public Settings.ReductionMode getReductionMode() {
    return reductionMode;
  }

  @Override
  public REPLSymbol replSymbol() {
    return REPLSymbol.REDUCTION_MODE;
  }
}
