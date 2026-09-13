{
  description = "zeno -- an always-on orchestrator for ephemeral agents";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
      forAll = f: nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});
    in
    {
      formatter = forAll (pkgs: pkgs.nixpkgs-fmt);

      devShells = forAll (pkgs: {
        default = pkgs.mkShell {
          packages = [ pkgs.clojure pkgs.jdk pkgs.rlwrap ];
        };
      });

      apps = forAll (pkgs:
        let
          zeno = pkgs.writeShellScriptBin "zeno" ''
            export PATH=${pkgs.jdk}/bin:${pkgs.clojure}/bin:$PATH
            CFG="$ZENO_HOME"
            if [ -z "$CFG" ]; then CFG="$HOME/.zeno"; fi
            DEPS="{:deps {io.github.reflection-dev/zeno {:local/root \"${self}\"} nrepl/nrepl {:mvn/version \"1.3.1\"} zeno/config {:local/root \"$CFG\"}}}"
            exec clojure -Sdeps "$DEPS" -M -m zeno.main "$@"
          '';
          app = { type = "app"; program = "${zeno}/bin/zeno"; };
        in { zeno = app; default = app; });
    };
}
