import 'package:flutter/widgets.dart';
import 'package:flutter_lucide/flutter_lucide.dart';

import '../data/institution_models.dart';

IconData institutionIconFor(InstitutionKind kind) {
  return switch (kind) {
    InstitutionKind.hospital => LucideIcons.hospital,
    InstitutionKind.pharmacy => LucideIcons.pill,
    InstitutionKind.emergency => LucideIcons.ambulance,
  };
}
