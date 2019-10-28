//#Patterns: strcpy

// Buggy �

main() {
 char x[1];
 //#Error: strcpy
 strcpy(x,"aaaa�");
}

