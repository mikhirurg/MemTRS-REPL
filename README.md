# MemTRS REPL

A simple REPL for experiments with Memory Term Rewriting Systems.

Based on the extension for the "cora" term rewriting analyzer tool (extension: https://github.com/mikhirurg/cora, original: https://github.com/hezzel/cora).

```shell
Welcome to MemTRS REPL v0.1
memtrs> include memtrs/stdlib/mem ds/array.lctrs
OK
memtrs> include memtrs/stdlib/algorithms/quicksort.lctrs
OK
memtrs> reduce listToArray(cons(3, cons(1, cons(5, cons(2, cons(4, nil))))))
Reduced to: 2
memtrs> print mem
Memory:
[8, 0, 5, 3, 1, 5, 2, 4, ...]
memtrs> reduce qsortArray(2, 0, 4)
Reduced to: true
memtrs> print mem
Memory:
[8, 0, 5, 1, 2, 3, 4, 5, ...]
memtrs>
```

Use command `help` to get information about available commands:
```shell
"reduce <term>": Reduce <term> to the normal form using all the available rules.
"trs": Prints current TRS.
"save <path>": Saves current TRS to the <path>.
"help": Prints information about available commands.
"info": Prints current rewriting engine settings,
"exit": Exit the MemTRS REPL.

"show_reductions <status>": Enables or disables the intermediate reductions trace. Options: [<status>: on/off].
"show_memory <status>": Enables or disables the global memory trace during reduction. Options: [<status>: on/off].

"reset_mem": Reset global memory.
"print_mem": Prints the global memory.
"resize_mem <new_size>": Resizes the global memory.
"print_array <addr>": Prints the array elements stored at the address <addr>.
"print_matrix <addr>": Prints the matrix elements stored at the address <addr>.

"reduction_mode <mode>": Updates the reduction mode for the term rewriting engine. Options: [<mode>: first/random/parallel].
"strategy <strategy>": Updates the reduction strategy for the term rewriting engine. Options: [<strategy>: full/innermost/cbv].

"include <path>": Import a script located at <path>.
"exclude <number>": Exclude a <number>'th script located at <path>.
"list_imports": Prints the current list of imported scripts.

"declare <term :: type>": Adds an extra declaration <term :: type> to the current list of temporary declarations.
"remove_declaration <number>": Removes the <number>'th declaration from the list of temporary declarations.
"list_declarations": Prints the current list of temporary declarations.
"clear_declarations": Clear the list of temporary declarations

"rule <l -> r | c>": Adds an extra rewriting rule <l -> r | c> to the current set of rules.
"remove_rule <number>": Removes the <number>'th rule from the list of temporary rewrite rules.
"clear_rules": Clear the list of temporary rules
"list_rules": Prints the current list of temporary rewrite rules.
```