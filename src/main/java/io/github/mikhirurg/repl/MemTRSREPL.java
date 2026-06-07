package io.github.mikhirurg.repl;

import charlie.parser.CoraParser;
import charlie.parser.Parser;
import charlie.reader.CoraInputReader;
import charlie.terms.FunctionSymbol;
import charlie.terms.Term;
import charlie.terms.TermFactory;
import charlie.terms.TermPrinter;
import charlie.terms.TheoryFactory;
import charlie.trs.Alphabet;
import charlie.trs.Rule;
import charlie.trs.TRS;
import charlie.trs.TrsFactory;
import cora.config.Settings;
import cora.reduction.MemReducer;
import cora.reduction.Reducer;
import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import io.github.mikhirurg.repl.commands.declarations.AddDeclaration;
import io.github.mikhirurg.repl.commands.declarations.ClearDeclarations;
import io.github.mikhirurg.repl.commands.declarations.ListDeclarations;
import io.github.mikhirurg.repl.commands.declarations.RemoveDeclaration;
import io.github.mikhirurg.repl.commands.general.Help;
import io.github.mikhirurg.repl.commands.general.Info;
import io.github.mikhirurg.repl.commands.general.ListTRS;
import io.github.mikhirurg.repl.commands.general.Save;
import io.github.mikhirurg.repl.commands.imports.ListImports;
import io.github.mikhirurg.repl.commands.memory.PrintArray;
import io.github.mikhirurg.repl.commands.memory.PrintMatrix;
import io.github.mikhirurg.repl.commands.memory.PrintMem;
import io.github.mikhirurg.repl.commands.memory.ResizeMem;
import io.github.mikhirurg.repl.commands.rules.AddRule;
import io.github.mikhirurg.repl.commands.imports.Exclude;
import io.github.mikhirurg.repl.commands.general.Exit;
import io.github.mikhirurg.repl.commands.imports.Include;
import io.github.mikhirurg.repl.commands.REPLCommand;
import io.github.mikhirurg.repl.commands.general.Reduce;
import io.github.mikhirurg.repl.commands.rewriting_settings.ReductionMode;
import io.github.mikhirurg.repl.commands.memory.ResetMem;
import io.github.mikhirurg.repl.commands.rules.ClearRules;
import io.github.mikhirurg.repl.commands.rewriting_settings.Strategy;
import io.github.mikhirurg.repl.commands.debug.ShowMemory;
import io.github.mikhirurg.repl.commands.debug.ShowReductions;
import io.github.mikhirurg.repl.commands.rules.ListRules;
import io.github.mikhirurg.repl.commands.rules.RemoveRule;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class MemTRSREPL {
  public static TRS currentTRS;
  public static List<String> includeList;
  public static List<Rule> ruleList;
  public static List<FunctionSymbol> declarationsList;

  private static Completer memTRSCompleter() {
    List<String> availableCommands = Arrays.stream(REPLCommand.REPLSymbol.values())
      .map(REPLCommand.REPLSymbol::getSymbol)
      .toList();

    Completers.FileNameCompleter fileNameCompleter = new Completers.FileNameCompleter();

    return (LineReader reader, ParsedLine line, List<Candidate> candidates) -> {
      List<String> words = line.words();
      if (line.wordIndex() == 0) {
        String current = line.word();
        availableCommands.stream()
          .filter(command -> command.startsWith(current))
          .map(Candidate::new)
          .forEach(candidates::add);
        return;
      }

      if (words.isEmpty()) {
        return;
      }

      String command = words.get(0);

      if (command.equals("help")) {
        String current = line.word();

        availableCommands.stream()
          .filter(name -> name.startsWith(current))
          .map(Candidate::new)
          .forEach(candidates::add);

        return;
      }

      if (command.equals("include") || command.equals("save")) {
        fileNameCompleter.complete(reader, line, candidates);
      }
    };
  }

  public static Term reduceToNF(Term start, TRS trs, Settings.Strategy strategy) {
    Settings.Strategy oldStrategy = Settings.queryRewritingStrategy();
    Settings.setStrategy(strategy);

    Reducer reducer = new Reducer(trs);
    Term s = start;
    Term oldS;
    do {
      oldS = s;
      s = reducer.reduce(s);
    } while (s != null);

    Settings.setStrategy(oldStrategy);
    return oldS;
  }

  private static boolean refreshTRS() {
    TRS oldTRS = currentTRS;

    try {
      currentTRS =
        CoraInputReader.readTrsFromString(includeList.stream()
          .map(include -> "#include " + include)
          .collect(Collectors.joining("\n")));

      Set<Rule> rules = new HashSet<>(currentTRS.queryRules());
      rules.addAll(ruleList);

      Set<FunctionSymbol> declarations = new HashSet<>(declarationsList);
      declarations.addAll(currentTRS.queryAlphabet().getSymbols());

      currentTRS = TrsFactory.createTrs(new Alphabet(declarations), rules.stream().toList(), TrsFactory.LCTRS);

      return true;
    } catch (Exception e) {
      currentTRS = oldTRS;
      System.out.println(TermPrinter.ANSICodes.RED + "Unable to construct new TRS! Rolling back " +
        "to the previous version!" + TermPrinter.ANSICodes.RESET);
      System.out.println(TermPrinter.ANSICodes.RED + e + TermPrinter.ANSICodes.RESET);
      return false;
    }
  }

  private static Term memoryDump() {
    Term dump = TheoryFactory.createValue(true);
    for (int i = 0; i < Settings.getMemMaxSize(); i++) {
      dump = TheoryFactory.createConjunction(dump,
        currentTRS.lookupSymbol("SET").apply(TheoryFactory.createValue(i)).apply(TheoryFactory.createValue(MemReducer.GET(i))));
    }
    return dump;
  }

  private static int[] arrayFromMem(int addr) {
    int[] arr = new int[MemReducer.GET(addr)];
    for (int k = addr + 1; k < arr.length + addr + 1; k++) {
      arr[k - (addr + 1)] = MemReducer.GET(k);
    }

    return arr;
  }

  public static void main(String[] args) throws IOException {
    Scanner in = new Scanner(System.in);

    MemReducer.resizeMemory(1000);
    Settings.setAnsiTermsHighlighting(true);

    System.out.println("Welcome to MemTRS REPL v0.1");

    includeList = new ArrayList<>();
    ruleList = new ArrayList<>();
    declarationsList = new ArrayList<>();

    currentTRS = TrsFactory.createTrs(new Alphabet(declarationsList), ruleList, TrsFactory.LCTRS);
    refreshTRS();

    Terminal terminal = TerminalBuilder.builder()
      .system(true)
      .build();

    LineReader reader = LineReaderBuilder.builder()
      .terminal(terminal)
      .appName("MemTRS")
      .completer(memTRSCompleter())
      .build();

    reader.setVariable(
      LineReader.HISTORY_FILE,
      Path.of(System.getProperty("user.home"), ".memtrs_history")
    );

    reader.setVariable(LineReader.HISTORY_SIZE, 1000);
    reader.setOpt(LineReader.Option.HISTORY_IGNORE_DUPS);
    reader.setOpt(LineReader.Option.HISTORY_IGNORE_SPACE);
    reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);

    REPLCommand parsedCommand;
    String prompt = TermPrinter.ANSICodes.CYAN + "memtrs> " + TermPrinter.ANSICodes.RESET;

    try {
      while (true) {
        String input;

        try {
          input = reader.readLine(prompt);
        } catch (UserInterruptException e) {
          continue;
        } catch (EndOfFileException e) {
          System.out.println();
          break;
        }

        input = input.trim();

        if (input.isEmpty()) {
          continue;
        }

        parsedCommand = REPLCommand.of(input);

        if (parsedCommand == null) {
          System.out.println(TermPrinter.ANSICodes.RED + "Unknown command! Try again!" + TermPrinter.ANSICodes.RESET);
          continue;
        }

        final String OK = TermPrinter.ANSICodes.GREEN + "OK" + TermPrinter.ANSICodes.GREEN;
        switch (parsedCommand) {
          // Debug
          case ShowMemory c -> {
            Settings.setShowIntermediateMemory(c.isEnabled());
            System.out.println(OK);
          }
          case ShowReductions c -> {
            Settings.setShowIntermediateReductions(c.isEnabled());
            System.out.println(OK);
          }

          // Declarations
          case AddDeclaration c -> {
            if (!declarationsList.contains(c.getDeclaration())) {
              declarationsList.add(c.getDeclaration());
              if (refreshTRS()) {
                System.out.println(OK);
              }
            } else {
              System.out.println("This function symbol declaration already exists!");
            }
          }
          case RemoveDeclaration c -> {
            if (c.getNumber() >= 1 && c.getNumber() <= declarationsList.size()) {
              declarationsList.remove(c.getNumber() - 1);
              if (refreshTRS()) {
                System.out.println(OK);
              }
            } else {
              System.out.println(TermPrinter.ANSICodes.RED + "Unable remove the function symbol " +
                "declaration #" + c.getNumber() + "!" + TermPrinter.ANSICodes.RESET);
            }
          }
          case ListDeclarations _ -> {
            System.out.println("Temporary symbol declarations:");
            for (int i = 1; i <= declarationsList.size(); i++) {
              System.out.println("#" + i + " " + declarationsList.get(i - 1).queryName() + " :: " + declarationsList.get(i - 1).queryType());
            }
          }
          case ClearDeclarations _ -> {
            System.out.println(OK);
            declarationsList.clear();
          }

          // General
          case Exit _ -> {
            System.out.println("Bye!");
            return;
          }
          case Help c -> {
            if (c.getCommand() != null) {
              Arrays.stream(REPLCommand.REPLSymbol.values())
                .filter(symb -> symb.getSymbol().equals(c.getCommand()))
                .findFirst()
                .ifPresentOrElse(symb -> System.out.println(symb.getDescription()),
                  () -> System.out.println(TermPrinter.ANSICodes.RED + "Unable to find help for " +
                    "command \"" + c.getCommand() + "\"." + TermPrinter.ANSICodes.RESET));
            } else {
              Arrays.stream(REPLCommand.REPLSymbol.values())
                .forEach(command -> System.out.println(command.getDescription()));
            }
          }
          case ListTRS _ -> {
            System.out.println(currentTRS.toString());
          }
          case Reduce c -> {
            Term result = reduceToNF(c.getTerm(), currentTRS, Settings.queryRewritingStrategy());
            System.out.println("Reduced to: " + result);
          }
          case Save c -> {
            try (var fw = new FileWriter(c.getPath())) {
              TRS oldTrs = currentTRS;

              Parser.ParserDeclaration declaration = CoraParser.readDeclaration("RESTORE_MEM :: " +
                "Bool", true, null);
              FunctionSymbol restoreMem = TermFactory.createConstant(declaration.name(),
                declaration.type());
              declarationsList.add(restoreMem);
              ruleList.add(TrsFactory.createRule(restoreMem, memoryDump()));
              refreshTRS();

              fw.append(currentTRS.queryAlphabet().toString().replace(":", "::"));
              fw.append(currentTRS.queryRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));

              currentTRS = oldTrs;
              declarationsList.removeLast();
              ruleList.removeLast();
            } catch (IOException e) {
              System.out.println(TermPrinter.ANSICodes.RED + "Unable to save MemTRS to file \"" + c.getPath() + "\"!" + TermPrinter.ANSICodes.RESET);
            } finally {
              System.out.println(OK);
            }
          }
          case Info _ -> {
            System.out.println("Rewriting engine settings:");
            System.out.println("=========== GENERAL ===========");
            System.out.println("Memory size: " + Settings.getMemMaxSize());
            System.out.println("Reduction mode: " + Settings.queryReductionMode());
            System.out.println("Rewriting strategy: " + Settings.queryRewritingStrategy());
            System.out.println("=========== DEBUG ===========");
            System.out.println("Show intermediate memory: " + Settings.isShowIntermediateMemory());
            System.out.println("Show intermediate reductions: " + Settings.isShowIntermediateReductions());
          }

          // Imports
          case Exclude c -> {
            if (c.getNumber() >= 1 && c.getNumber() <= includeList.size()) {
              includeList.remove(c.getNumber() - 1);
              if (refreshTRS()) {
                System.out.println(OK);
              }
            } else {
              System.out.println(TermPrinter.ANSICodes.RED + "Unable exclude the input script #" + c.getNumber() + "!" + TermPrinter.ANSICodes.RESET);
            }
          }
          case Include command -> {
            if (!includeList.contains(command.getPath())) {
              String absolutePath = Path.of(command.getPath()).toAbsolutePath().toString();
              includeList.add(absolutePath);
              if (refreshTRS()) {
                System.out.println(OK);
              } else {
                includeList.remove(absolutePath);
              }
            } else {
              System.out.println(TermPrinter.ANSICodes.RED + "This script is already imported!" + TermPrinter.ANSICodes.RESET);
            }
          }
          case ListImports _ -> {
            System.out.println("External script imports:");
            for (int i = 1; i <= includeList.size(); i++) {
              System.out.println("#" + i + " " + includeList.get(i - 1));
            }
          }

          // Memory
          case PrintMem _ -> {
            System.out.println("Memory:");
            System.out.println(MemReducer.MEMORY);
          }
          case ResetMem _ -> {
            MemReducer.resetMemory();
            System.out.println(OK);
          }
          case ResizeMem c -> {
            if (c.getSize() < 2) {
              System.out.println(TermPrinter.ANSICodes.RED + "Memory size is too small!" + TermPrinter.ANSICodes.RESET);
            } else {
              MemReducer.resizeMemory(c.getSize());
              System.out.println(OK);
            }
          }
          case PrintArray c -> {
            System.out.println("Array:");
            System.out.println(Arrays.toString(arrayFromMem(c.getAddr())));
          }
          case PrintMatrix c -> {
            System.out.println("Matrix:");
            int[] headerArray = arrayFromMem(c.getAddr());
            int[][] rows = new int[headerArray.length][];
            int maxLen = 0;
            for (int i = 0; i < headerArray.length; i++) {
              rows[i] = arrayFromMem(headerArray[i]);
              maxLen = Math.max(maxLen, Arrays.toString(rows[i]).length() - 2);
            }

            int columnCount = Arrays.stream(rows)
              .mapToInt(row -> row.length)
              .max()
              .orElse(0);

            int[] columnWidths = new int[columnCount];

            for (int[] row : rows) {
              for (int j = 0; j < row.length; j++) {
                columnWidths[j] = Math.max(
                  columnWidths[j],
                  String.valueOf(row[j]).length()
                );
              }
            }

            int innerWidth = Arrays.stream(columnWidths).sum()
              + Math.max(0, columnCount - 1) * 2;

            String border = "+" + "-".repeat(innerWidth + 2) + "+";

            System.out.println(border);

            for (int[] row : rows) {
              StringBuilder line = new StringBuilder("| ");

              for (int j = 0; j < row.length; j++) {
                String value = String.valueOf(row[j]);

                line.repeat(" ", columnWidths[j] - value.length());
                line.append(value);

                if (j < row.length - 1) {
                  line.append(", ");
                }
              }

              line.repeat(" ", innerWidth - line.length() + 2);
              line.append(" |");

              System.out.println(line);
            }

            System.out.println(border);
          }

          // Rewriting settings:
          case ReductionMode c -> {
            Settings.setReductionMode(c.getReductionMode());
            System.out.println(OK);
          }
          case Strategy c -> {
            Settings.setStrategy(c.getStrategy());
            System.out.println(OK);
          }

          // Rules
          case AddRule c -> {
            if (!ruleList.contains(c.getRule())) {
              ruleList.add(c.getRule());
              if (refreshTRS()) {
                System.out.println(OK);
              }
            } else {
              System.out.println(TermPrinter.ANSICodes.RED + "This rule already exists!" + TermPrinter.ANSICodes.RESET);
            }
          }
          case ClearRules _ -> {
            ruleList.clear();
            if (refreshTRS()) {
              System.out.println(OK);
            }
          }
          case RemoveRule c -> {
            if (c.getNumber() >= 1 && c.getNumber() <= ruleList.size()) {
              ruleList.remove(c.getNumber());
              if (refreshTRS()) {
                System.out.println(OK);
              }
            } else {
              System.out.println(TermPrinter.ANSICodes.RED + "Unable exclude the rule #" + c.getNumber() + "!" + TermPrinter.ANSICodes.RESET);
            }
          }
          case ListRules _ -> {
            System.out.println("Temporary rule declarations:");
            for (int i = 1; i <= ruleList.size(); i++) {
              System.out.println("#" + i + " " + ruleList.get(i - 1));
            }
          }
          default -> throw new IllegalStateException("Unexpected value: " + parsedCommand);
        }
      }
    } finally {
      reader.getHistory().save();
      terminal.close();
    }
  }
}
