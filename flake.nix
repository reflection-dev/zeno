{
  description = "zeno -- an always-on orchestrator for ephemeral agents";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  # secretspec pinned to a fixed nixpkgs rev: its binary (hence the macOS keychain
  # trust of already-stored secrets) stays byte-identical across nixpkgs bumps, so
  # updating zeno never invalidates secrets. Bump this rev only deliberately.
  inputs.secretspec-pkgs.url = "github:NixOS/nixpkgs/8ce4ef6cb6f871616146b9fe26d2a5ae594e94fe";

  outputs = { self, nixpkgs, secretspec-pkgs }:
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

      apps = nixpkgs.lib.genAttrs systems (system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          secretspec = secretspec-pkgs.legacyPackages.${system}.secretspec;
          zeno = pkgs.writeShellScriptBin "zeno" ''
            export PATH=${pkgs.jdk}/bin:${pkgs.clojure}/bin:${secretspec}/bin:$PATH
            CFG="$ZENO_HOME"
            if [ -z "$CFG" ]; then CFG="$HOME/.zeno"; fi

            # Secrets. The launcher loads the instance's secretspec profile into
            # the env before starting, so the instance reads its secrets from the
            # environment (like ~/.emacs.d relying on the ambient env). Opt out
            # with --no-secretspec or ZENO_SECRETSPEC=0; an instance with no
            # secretspec.toml at $ZENO_HOME just runs raw. zeno itself never names
            # a store - this is only the launcher priming the env.
            #
            # `zeno secretspec ...` is a passthrough to the SAME secretspec this
            # launcher reads with, run in $ZENO_HOME. Manage an instance's secrets
            # through it (e.g. `zeno secretspec set FOO`) so every item is created
            # and read by one binary: macOS keychain trusts an item per-binary, so
            # setting it with a different secretspec would make a headless/tmux
            # launch stall on a keychain trust prompt.
            if [ "$1" = "secretspec" ]; then
              shift; cd "$CFG" || exit 1; exec secretspec "$@"
            fi
            if [ "$1" = "--no-secretspec" ]; then shift; ZENO_SECRETSPEC=0; fi
            if [ -z "$ZENO_UNDER_SECRETSPEC" ] && [ "$ZENO_SECRETSPEC" != "0" ] && [ -f "$CFG/secretspec.toml" ]; then
              ZENO_UNDER_SECRETSPEC=1; export ZENO_UNDER_SECRETSPEC
              cd "$CFG" || exit 1
              exec secretspec run --reason zeno -- "$0" "$@"
            fi

            DEPS="{:deps {io.github.reflection-dev/zeno {:local/root \"${self}\"} nrepl/nrepl {:mvn/version \"1.3.1\"} zeno/config {:local/root \"$CFG\"}}}"
            exec clojure -Sdeps "$DEPS" -M -m zeno.main "$@"
          '';
          app = { type = "app"; program = "${zeno}/bin/zeno"; };
        in { zeno = app; default = app; });
    };
}
