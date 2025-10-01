# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release of MineZero.
- Checkpoint system that saves player and world state.
- Anchor player system to trigger world resets.
- `/setcheckpoint` command for manual checkpoint creation.
- Artifact Flute item for instant checkpoint setting.
- Custom `death_chime` sound on world reset.
- Support for multiple Minecraft versions and mod loaders:
    - `1.20.1-forge`
    - `1.21.1-neoforge`
    - `1.20.1-fabric`
    - `1.12.2-forge`