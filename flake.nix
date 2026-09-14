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
            export PATH=${pkgs.jdk}/bin:${pkgs.clojure}/bin:${pkgs.secretspec}/bin:$PATH
            CFG="$ZENO_HOME"
            if [ -z "$CFG" ]; then CFG="$HOME/.zeno"; fi

            # Secrets. The launcher loads the instance's secretspec profile into
            # the env before starting, so the instance reads its secrets from the
            # environment (like ~/.emacs.d relying on the ambient env). Opt out
            # with --no-secretspec or ZENO_SECRETSPEC=0; an instance with no
            # secretspec.toml at $ZENO_HOME just runs raw. zeno itself never names
            # a store - this is only the launcher priming the env.
            #
            # Backend: if $ZENO_HOME/.env exists, secretspec reads secrets from
            # that file (dotenv provider) - any binary reads a file, no macOS
            # keychain is touched, so a headless/SSH/tmux launch never stalls on a
            # keychain trust prompt. Otherwise the default provider (keyring) is
            # used. Either way secrets are still validated against secretspec.toml.
            if [ "$1" = "--no-secretspec" ]; then shift; ZENO_SECRETSPEC=0; fi
            if [ -z "$ZENO_UNDER_SECRETSPEC" ] && [ "$ZENO_SECRETSPEC" != "0" ] && [ -f "$CFG/secretspec.toml" ]; then
              ZENO_UNDER_SECRETSPEC=1; export ZENO_UNDER_SECRETSPEC
              cd "$CFG" || exit 1
              if [ -f "$CFG/.env" ]; then
                exec secretspec run --provider dotenv --reason zeno -- "$0" "$@"
              else
                exec secretspec run --reason zeno -- "$0" "$@"
              fi
            fi

            DEPS="{:deps {io.github.reflection-dev/zeno {:local/root \"${self}\"} nrepl/nrepl {:mvn/version \"1.3.1\"} zeno/config {:local/root \"$CFG\"}}}"
            exec clojure -Sdeps "$DEPS" -M -m zeno.main "$@"
          '';
          app = { type = "app"; program = "${zeno}/bin/zeno"; };
        in { zeno = app; default = app; });
    };
}
