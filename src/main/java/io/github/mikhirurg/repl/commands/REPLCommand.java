package io.github.mikhirurg.repl.commands;

import charlie.parser.CoraParser;
import charlie.parser.Parser;
import charlie.reader.CoraInputReader;
import charlie.terms.TermFactory;
import charlie.terms.TermPrinter;
import cora.config.Settings;
import io.github.mikhirurg.repl.MemTRSREPL;
import io.github.mikhirurg.repl.commands.debug.ShowMemory;
import io.github.mikhirurg.repl.commands.debug.ShowReductions;
import io.github.mikhirurg.repl.commands.declarations.AddDeclaration;
import io.github.mikhirurg.repl.commands.declarations.ClearDeclarations;
import io.github.mikhirurg.repl.commands.declarations.ListDeclarations;
import io.github.mikhirurg.repl.commands.declarations.RemoveDeclaration;
import io.github.mikhirurg.repl.commands.general.Exit;
import io.github.mikhirurg.repl.commands.general.Help;
import io.github.mikhirurg.repl.commands.general.Info;
import io.github.mikhirurg.repl.commands.general.Reduce;
import io.github.mikhirurg.repl.commands.general.Save;
import io.github.mikhirurg.repl.commands.general.ListTRS;
import io.github.mikhirurg.repl.commands.imports.Exclude;
import io.github.mikhirurg.repl.commands.imports.Include;
import io.github.mikhirurg.repl.commands.imports.ListImports;
import io.github.mikhirurg.repl.commands.memory.PrintArray;
import io.github.mikhirurg.repl.commands.memory.PrintMatrix;
import io.github.mikhirurg.repl.commands.memory.PrintMem;
import io.github.mikhirurg.repl.commands.memory.ResetMem;
import io.github.mikhirurg.repl.commands.memory.ResizeMem;
import io.github.mikhirurg.repl.commands.rewriting_settings.ReductionMode;
import io.github.mikhirurg.repl.commands.rewriting_settings.Strategy;
import io.github.mikhirurg.repl.commands.rules.AddRule;
import io.github.mikhirurg.repl.commands.rules.ClearRules;
import io.github.mikhirurg.repl.commands.rules.ListRules;
import io.github.mikhirurg.repl.commands.rules.RemoveRule;

public interface REPLCommand {

  static String replaceLast(String text, String regex, String replacement) {
    return text.replaceFirst("(?s)"+regex+"(?!.*?"+regex+")", replacement);
  }

  enum REPLSymbol {
    // General:
    REDUCE("reduce", "\"reduce <term>\": Reduce <term> to the normal form using all the available" +
      " rules."),
    TRS("trs", "\"trs\": Prints current TRS."),
    SAVE("save", "\"save <path>\": Saves current TRS to the <path>."),
    HELP("help", "\"help\": Prints information about available commands."),
    INFO("info", "\"info\": Prints current rewriting engine settings,"),
    EXIT("exit", "\"exit\": Exit the MemTRS REPL."),

    // Debug:,
    SHOW_REDUCTIONS("show_reductions", "\"show_reductions <status>\": Enables or disables the " +
      "intermediate reductions trace. Options: [<status>: on/off]."),
    SHOW_MEMORY("show_memory", "\"show_memory <status>\": Enables or disables the global memory " +
      "trace during reduction. Options: [<status>: on/off]."),

    // Memory:,
    RESET_MEM("reset_mem", "\"reset_mem\": Reset global memory."),
    PRINT_MEM("print_mem", "\"print_mem\": Prints the global memory."),
    RESIZE_MEM("resize_mem", "\"resize_mem <new_size>\": Resizes the global memory."),
    PRINT_ARRAY("print_array", "\"print_array <addr>\": Prints the array elements stored at the " +
      "address <addr>."),
    PRINT_MATRIX("print_matrix", "\"print_matrix <addr>\": Prints the matrix elements stored at " +
      "the address <addr>."),

    // Rewriting settings:,
    REDUCTION_MODE("reduction_mode", "\"reduction_mode <mode>\": Updates the reduction mode for " +
      "the term rewriting engine. Options: [<mode>: first/random/parallel]."),
    STRATEGY("strategy", "\"strategy <strategy>\": Updates the reduction strategy for the term " +
      "rewriting engine. Options: [<strategy>: full/innermost/cbv]."),

    // Imports:
    INCLUDE("include", "\"include <path>\": Import a script located at <path>."),
    EXCLUDE("exclude", "\"exclude <number>\": Exclude a <number>'th script located at <path>."),
    LIST_IMPORTS("list_imports", "\"list_imports\": Prints the current list of imported scripts."),

    // Declarations:
    DECLARE("declare", "\"declare <term :: type>\": Adds an extra declaration " +
      "<term :: type> to the current list of temporary declarations."),
    REMOVE_DECLARATION("remove_declaration", "\"remove_declaration <number>\": Removes the " +
      "<number>'th declaration from the list of temporary declarations."),
    LIST_DECLARATIONS("list_declarations", "\"list_declarations\": Prints the current list of " +
      "temporary declarations."),
    CLEAR_DECLARATIONS("clear_declarations", "\"clear_declarations\": Clear the list of temporary" +
      " declarations"),

    // Rules:
    RULE("rule", "\"rule <l -> r | c>\": Adds an extra rewriting rule <l -> r | c> to the " +
      "current set of rules."),
    REMOVE_RULE("remove_rule", "\"remove_rule <number>\": Removes the <number>'th rule from the " +
      "list of temporary rewrite rules."),
    CLEAR_RULES("clear_rules", "\"clear_rules\": Clear the list of temporary rules"),
    LIST_RULES("list_rules", "\"list_rules\": Prints the current list of temporary rewrite " +
      "rules.");

    private final String symbol;
    private final String description;

    REPLSymbol(String symbol, String description) {
      this.symbol = symbol;
      description = description.replaceFirst("\"", "\"" + TermPrinter.ANSICodes.GREEN);
      description = replaceLast(description, "\"", TermPrinter.ANSICodes.RESET + "\"");
      this.description = description;
    }

    public String getSymbol() {
      return symbol;
    }

    public String getDescription() {
      return description;
    }
  }

  static REPLCommand of(String input) {

    int spaceBegin = input.indexOf(' ');
    if (spaceBegin == -1) {
      spaceBegin = input.length();
    }

    String command = input.substring(0, spaceBegin).trim();
    String argument = input.substring(spaceBegin).trim();

    if (argument.isEmpty()) {
      argument = null;
    }

    // Debug:
    if (REPLSymbol.SHOW_MEMORY.symbol.equals(command)) {
      return (argument != null ? switch (argument) {
        case "on" -> new ShowMemory(true);
        case "off" -> new ShowMemory(false);
        default -> null;
      } : null);
    } else if (REPLSymbol.SHOW_REDUCTIONS.symbol.equals(command)) {
      return argument != null ?
        switch (argument) {
          case "on" -> new ShowReductions(true);
          case "off" -> new ShowReductions(false);
          default -> null;
        } : null;

      // Declarations:
    } else if (REPLSymbol.DECLARE.symbol.equals(command)) {
      if (argument != null) {
        try {
          Parser.ParserDeclaration declaration = CoraParser.readDeclaration(argument, true, null);
          return new AddDeclaration(TermFactory.createConstant(declaration.name(),
            declaration.type()));
        } catch (Exception e) {
          System.err.println(e.getMessage());
          return null;
        }
      } else {
        return null;
      }
    } else if (REPLSymbol.REMOVE_DECLARATION.symbol.equals(command)) {
      if (argument == null) return null;
      try {
        int arg = Integer.parseInt(argument);
        return new RemoveDeclaration(arg);
      } catch (NumberFormatException e) {
        return null;
      }
    } else if (REPLSymbol.LIST_DECLARATIONS.symbol.equals(command)) {
      return new ListDeclarations();
    } else if (REPLSymbol.CLEAR_DECLARATIONS.symbol.equals(command)) {
      return new ClearDeclarations();

      // General:
    } else if (REPLSymbol.EXIT.symbol.equals(command)) {
      return new Exit();
    } else if (REPLSymbol.HELP.symbol.equals(command)) {
      if (argument != null) {
        return new Help(argument);
      } else  {
        return new Help();
      }
    } else if (REPLSymbol.REDUCE.symbol.equals(command)) {
      try {
        return argument != null ?
          new Reduce(CoraInputReader.readTerm(argument,
            MemTRSREPL.currentTRS)) : null;
      } catch (Exception e) {
        System.err.println(e.getMessage());
        return null;
      }
    } else if (REPLSymbol.SAVE.symbol.equals(command)) {
      return argument != null ? new Save(argument) : null;
    } else if (REPLSymbol.TRS.symbol.equals(command)) {
      return new ListTRS();
    } else if (REPLSymbol.INFO.symbol.equals(command)) {
      return new Info();

      // Imports:
    } else if (REPLSymbol.EXCLUDE.symbol.equals(command)) {
      if (argument == null) return null;
      try {
        int arg = Integer.parseInt(argument);
        return new Exclude(arg);
      } catch (NumberFormatException e) {
        return null;
      }
    } else if (REPLSymbol.INCLUDE.symbol.equals(command)) {
      return argument != null ? new Include(argument) : null;
    } else if (REPLSymbol.LIST_IMPORTS.symbol.equals(command)) {
      return new ListImports();

      // Memory:
    } else if (REPLSymbol.PRINT_MEM.symbol.equals(command)) {
      return new PrintMem();
    } else if (REPLSymbol.RESET_MEM.symbol.equals(command)) {
      return new ResetMem();
    } else if (REPLSymbol.RESIZE_MEM.symbol.equals(command)) {
      if (argument == null) return null;
      try {
        int arg = Integer.parseInt(argument);
        return new ResizeMem(arg);
      } catch (NumberFormatException e) {
        return null;
      }
    } else if (REPLSymbol.PRINT_ARRAY.symbol.equals(command)) {
      if (argument == null) return null;
      try {
        int arg = Integer.parseInt(argument);
        return new PrintArray(arg);
      } catch (NumberFormatException e) {
        return null;
      }
    } else if (REPLSymbol.PRINT_MATRIX.symbol.equals(command)) {
      if (argument == null) return null;
      try {
        int arg = Integer.parseInt(argument);
        return new PrintMatrix(arg);
      } catch (NumberFormatException e) {
        return null;
      }

      // Rewrite Settings:
    } else if (REPLSymbol.REDUCTION_MODE.symbol.equals(command)) {
      return argument != null ? switch (argument) {
        case "first" -> new ReductionMode(Settings.ReductionMode.FirstMatch);
        case "random" -> new ReductionMode(Settings.ReductionMode.Random);
        case "parallel" -> new ReductionMode(Settings.ReductionMode.Parallel);
        default -> null;
      } : null;
    } else if (REPLSymbol.STRATEGY.symbol.equals(command)) {
      return argument != null ? switch (argument) {
        case "full" -> new Strategy(Settings.Strategy.Full);
        case "innermost" -> new Strategy(Settings.Strategy.Innermost);
        case "cbv" -> new Strategy(Settings.Strategy.CallByValue);
        default -> null;
      } : null;

      // Rules:
    } else if (REPLSymbol.RULE.symbol.equals(command)) {
      try {
        return new AddRule(CoraInputReader.readRule(argument, MemTRSREPL.currentTRS));
      } catch (Exception e) {
        System.err.println(e.getMessage());
        return null;
      }
    } else if (REPLSymbol.CLEAR_RULES.symbol.equals(command)) {
      return new ClearRules();
    } else if (REPLSymbol.LIST_RULES.symbol.equals(command)) {
      return new ListRules();
    } else if (REPLSymbol.REMOVE_RULE.symbol.equals(command)) {
      if (argument == null) return null;
      try {
        int arg = Integer.parseInt(argument);
        new RemoveRule(arg);
      } catch (NumberFormatException e) {
        return null;
      }
    } else {
      return null;
    }
    return null;
  }

  REPLSymbol replSymbol();
}
