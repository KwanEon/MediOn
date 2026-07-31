class NoticeItem {
  const NoticeItem({
    required this.id,
    required this.category,
    required this.title,
    required this.content,
    required this.pinned,
    required this.publishedAt,
    required this.updatedAt,
  });

  factory NoticeItem.fromJson(Map<String, dynamic> json) {
    return NoticeItem(
      id: (json['id'] as num).toInt(),
      category: json['category'] as String,
      title: json['title'] as String,
      content: json['content'] as String,
      pinned: json['pinned'] as bool,
      publishedAt: DateTime.parse(json['publishedAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
    );
  }

  final int id;
  final String category;
  final String title;
  final String content;
  final bool pinned;
  final DateTime publishedAt;
  final DateTime updatedAt;
}

class InquiryItem {
  const InquiryItem({
    required this.id,
    required this.category,
    required this.title,
    required this.content,
    required this.status,
    required this.createdAt,
    required this.updatedAt,
  });

  factory InquiryItem.fromJson(Map<String, dynamic> json) {
    return InquiryItem(
      id: (json['id'] as num).toInt(),
      category: json['category'] as String,
      title: json['title'] as String,
      content: json['content'] as String,
      status: json['status'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
    );
  }

  final int id;
  final String category;
  final String title;
  final String content;
  final String status;
  final DateTime createdAt;
  final DateTime updatedAt;
}
