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
    };
}
